package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import timber.log.Timber;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;
    private final List<Launchable> allLaunchables;
    private List<Launchable> filteredLaunchables;

    /**
     * Load a {@link Drawable} from a {@link Launchable} and update the {@link ImageView}.
     * <p>
     * Getting the Drawable from the Launchable can be slow, this class helps doing that in the
     * background.
     */
    private static class AsyncSetImageDrawable extends AsyncTask<Launchable, Void, Drawable> {
        private final WeakReference<ImageView> imageViewReference;

        public AsyncSetImageDrawable(ImageView imageView) {
            this.imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Drawable doInBackground(Launchable... launchables) {
            return launchables[0].getIcon();
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            ImageView imageView = imageViewReference.get();
            if (imageView == null) {
                return;
            }
            imageView.setImageDrawable(drawable);
        }
    }

    public LaunchableAdapter(Context context, File statsFile, File nameCacheFile) {
        this.context = context;
        Timer timer = new Timer();
        timer.addLeg("Getting Launchables");
        allLaunchables = loadLaunchables(context);
        try {
            DatabaseUtils.nameLaunchablesFromCache(nameCacheFile, allLaunchables);
        } catch (IOException e) {
            Timber.w(e, "Updating names from cache failed");
        }

        dropUnnamed(allLaunchables);

        timer.addLeg("Sorting Launchables");
        DatabaseUtils.scoreLaunchables(statsFile, allLaunchables);
        Collections.sort(allLaunchables);

        timer.addLeg("Updating names cache");
        updateNamesCache(nameCacheFile);

        Timber.i("LaunchableAdapter timings: %s", timer);

        filteredLaunchables = allLaunchables;
    }

    private static void dropUnnamed(List<Launchable> launchables) {
        Iterator<Launchable> iterator = launchables.iterator();
        while (iterator.hasNext()) {
            Launchable launchable = iterator.next();
            if (launchable.getName().isEmpty()) {
                iterator.remove();
            } else if (launchable.id.equals("com.android.settings.com.samsung.android.settings.powersaving.PowerModeChangeDialogActivity")) {
                // This just unconditionally disables battery saving mode on my Galaxy S6, and it's
                // called "Battery" so having this in the list is just confusing.
                iterator.remove();
            }
        }
    }

    private void updateNamesCache(File nameCacheFile) {
        new Thread(() -> {
            try {
                DatabaseUtils.cacheTrueNames(nameCacheFile, allLaunchables);
                Timber.i("True names cached into %s", nameCacheFile.getAbsolutePath());
            } catch (IOException e) {
                Timber.w(e, "Caching names failed");
            }
        }, "Name Cache Updater").start();
    }

    @Override
    public int getCount() {
        return filteredLaunchables.size();
    }

    @Override
    public Object getItem(int i) {
        return filteredLaunchables.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater =
                    (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            view = inflater.inflate(R.layout.launchable, parent, false);
        }

        Launchable launchable = (Launchable)getItem(i);
        TextView textView = view.findViewById(R.id.launchableName);
        textView.setText(launchable.getName());
        ImageView imageView = view.findViewById(R.id.launchableIcon);
        new AsyncSetImageDrawable(imageView).execute(launchable);

        return view;
    }

    /**
     * @return A collection of launchables. No duplicate IDs, but zero or more Launchables may have
     * empty names.
     */
    static List<Launchable> loadLaunchables(Context context) {
        Timer timer = new Timer();
        final PackageManager packageManager = context.getPackageManager();

        timer.addLeg("Listing Query Intents");
        List<ResolveInfo> resInfos = new LinkedList<>();
        for (Intent intent: getQueryIntents()) {
            resInfos.addAll(packageManager.queryIntentActivities(intent, 0));
        }

        timer.addLeg("Creating Launchables");
        List<Launchable> launchables = new ArrayList<>();
        Set<String> doneIds = new HashSet<>();
        for(ResolveInfo resolveInfo : resInfos) {
            Launchable launchable = new Launchable(resolveInfo, packageManager);
            if (doneIds.contains(launchable.id)) {
                // Should we print a warning here? All dups we have looked at have been identical,
                // so just dropping these should be fine.
                continue;
            }

            launchables.add(launchable);
            doneIds.add(launchable.id);
        }

        Timber.i("loadLaunchables() timings: %s", timer);
        return launchables;
    }

    private static Iterable<Intent> getQueryIntents() {
        List<Intent> queryIntents = new LinkedList<>();

        Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        queryIntents.add(queryIntent);

        for (String action: getActions(Settings.class)) {
            queryIntent = new Intent(action);
            queryIntents.add(queryIntent);
        }

        // Battery screen on my Galaxy S6, I want to find this when searching for "battery"
        queryIntents.add(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));

        return queryIntents;
    }

    private static Iterable<String> getActions(Class<?> classWithConstants) {
        List<String> actions = new LinkedList<>();
        for (Field field: classWithConstants.getFields()) {
            if (!Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            if (field.getType() != String.class) {
                continue;
            }
            if (!field.getName().startsWith("ACTION_")) {
                continue;
            }

            String value;
            try {
                value = (String)field.get(null);
            } catch (IllegalAccessException e) {
                Timber.w(e, "Unable to get field value of %s.%s",
                        classWithConstants.getName(), field.getName());
                continue;
            }

            if (!value.endsWith("_SETTINGS")) {
                continue;
            }

            actions.add(value);
        }

        return actions;
    }

    public void setFilter(CharSequence search) {
        if (search.length() == 0) {
            filteredLaunchables = allLaunchables;
        }

        List<Launchable> newFilteredList = new LinkedList<>();
        for (Launchable launchable: allLaunchables) {
            if (launchable.matches(search.toString().toLowerCase(Locale.getDefault()))) {
                newFilteredList.add(launchable);
            }
        }

        filteredLaunchables = newFilteredList;

        notifyDataSetChanged();
    }
}
