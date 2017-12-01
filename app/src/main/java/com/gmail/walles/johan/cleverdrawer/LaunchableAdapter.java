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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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

        timer.addLeg("Sorting Launchables");
        Comparator<Launchable> comparator;
        try {
            comparator = Statistics.getComparator(statsFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed loading statistics", e);
        }

        Collections.sort(allLaunchables, comparator);

        updateNamesCache(nameCacheFile);

        Timber.i("LaunchableAdapter timings: %s", timer);

        filteredLaunchables = allLaunchables;
    }

    private void updateNamesCache(File nameCacheFile) {
        new Thread(() -> {
            try {
                DatabaseUtils.cacheTrueNames(nameCacheFile, allLaunchables);
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

    private static List<Launchable> loadLaunchables(Context context) {
        Timer timer = new Timer();
        final PackageManager packageManager = context.getPackageManager();

        timer.addLeg("Listing Query Intents");
        List<ResolveInfo> resInfos = new LinkedList<>();
        for (Intent intent: getQueryIntents()) {
            resInfos.addAll(packageManager.queryIntentActivities(intent, 0));
        }

        timer.addLeg("Creating Launchables");
        List<Launchable> launchables = new ArrayList<>();
        for(ResolveInfo resolveInfo : resInfos) {
            launchables.add(new Launchable(resolveInfo, packageManager));
        }

        Timber.i("getLaunchables() timings: %s", timer);
        return launchables;
    }

    private static Iterable<Intent> getQueryIntents() {
        List<Intent> queryIntents = new LinkedList<>();

        Intent appIntent = new Intent(Intent.ACTION_MAIN, null);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        queryIntents.add(appIntent);

        // List from decompiling Settings.java and filtering it:
        //
        // grep "public static final String ACTION_" Settings.java |sort|grep _SETTINGS|awk '{print "        queryIntents.add(new Intent(Settings." $5 "));"}'
        //
        // Then I manually removed the ones that Android Studio complained about.
        //
        // FIXME: Maybe get these through reflection instead?
        queryIntents.add(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_APN_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_DATE_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_DREAM_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_LOCALE_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_MEMORY_CARD_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_NFC_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_PRINT_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_PRIVACY_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_QUICK_LAUNCH_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_SEARCH_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_SOUND_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_SYNC_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_USER_DICTIONARY_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_WIFI_SETTINGS));
        queryIntents.add(new Intent(Settings.ACTION_WIRELESS_SETTINGS));

        return queryIntents;
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
