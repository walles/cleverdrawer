package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import java.util.List;

import timber.log.Timber;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;
    private final List<Launchable> launchables;

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
        launchables = getLaunchables(context);
        try {
            DatabaseUtils.nameLaunchablesFromCache(nameCacheFile, launchables);
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

        Collections.sort(launchables, comparator);

        updateNamesCache(nameCacheFile);

        Timber.i("LaunchableAdapter timings: %s", timer);
    }

    private void updateNamesCache(File nameCacheFile) {
        new Thread(() -> {
            try {
                DatabaseUtils.cacheTrueNames(nameCacheFile, launchables);
            } catch (IOException e) {
                Timber.w(e, "Caching names failed");
            }
        }, "Name Cache Updater").start();
    }

    @Override
    public int getCount() {
        return launchables.size();
    }

    @Override
    public Object getItem(int i) {
        return launchables.get(i);
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

    private static List<Launchable> getLaunchables(Context context) {
        Timer timer = new Timer();
        final PackageManager packageManager = context.getPackageManager();

        Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        queryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(queryIntent, 0);

        timer.addLeg("Creating Launchables");
        List<Launchable> launchables = new ArrayList<>();
        for(ResolveInfo resolveInfo : resInfos) {
            launchables.add(new Launchable(resolveInfo, packageManager));
        }

        Timber.i("getLaunchables() timings: %s", timer);
        return launchables;
    }
}
