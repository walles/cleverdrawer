/*
 * MIT License
 *
 * Copyright (c) 2017 Johan Walles <johan.walles@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.gmail.walles.johan.cleverdrawer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;
import timber.log.Timber;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;
    private final File launchHistoryFile;
    private final File nameCacheFile;
    private final File lastOrderFile;

    private List<Launchable> allLaunchables;
    private List<Launchable> filteredLaunchables;

    /**
     * Load a {@link Drawable} from a {@link Launchable} and update the {@link ImageView}.
     * <p>
     * Getting the Drawable from the Launchable can be slow, this class helps doing that in the
     * background.
     */
    private static class AsyncSetImageDrawable extends AsyncTask<Launchable, Void, Drawable> {
        private final WeakReference<ImageView> imageViewReference;

        protected AsyncSetImageDrawable(ImageView imageView) {
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

    public LaunchableAdapter(MainActivity mainActivity,
            File launchHistoryFile, File nameCacheFile, File lastOrderFile)
    {
        this.context = mainActivity;
        this.launchHistoryFile = launchHistoryFile;
        this.nameCacheFile = nameCacheFile;
        this.lastOrderFile = lastOrderFile;
        reloadLaunchables();

        mainActivity.setLaunchableAdapter(this);
    }

    public void reloadLaunchables() {
        allLaunchables = loadLaunchables(context, nameCacheFile, launchHistoryFile, lastOrderFile);
        filteredLaunchables = allLaunchables;
        notifyDataSetChanged();
    }

    static List<Launchable> loadLaunchables(Context context,
            File nameCacheFile, File launchHistoryFile, File lastOrderFile)
    {
        Timer timer = new Timer();
        ExecutorService executor = new ThreadPoolExecutor(
                2,
                4,
                500, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5));

        FutureTask<List<Launchable>> loadIntentLaunchables = new FutureTask<>(
                () -> IntentLaunchable.loadLaunchables(context));
        executor.submit(loadIntentLaunchables);

        FutureTask<Map<String, String>> readCache = new FutureTask<>(
                () -> DatabaseUtils.readIdToNameCache(nameCacheFile));
        executor.submit(readCache);

        FutureTask<List<DatabaseUtils.LaunchMetadata>> loadLaunchHistory = new FutureTask<>(
                () -> DatabaseUtils.loadLaunches(launchHistoryFile));
        executor.submit(loadLaunchHistory);

        List<Launchable> launchables = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED)
        {
            timer.addLeg("Add ContactLaunchables");
            launchables.addAll(ContactLaunchable.loadLaunchables(context));
        } else {
            Timber.w("READ_CONTACTS permission not granted (yet?), contacts not loaded");
        }

        timer.addLeg("Add IntentLaunchables");
        try {
            launchables.addAll(loadIntentLaunchables.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Loading Intent Launchables failed", e);
        }

        timer.addLeg("Dropping duplicate IDs");
        dropDuplicateIds(launchables);

        timer.addLeg("Applying names from cache");
        try {
            DatabaseUtils.nameLaunchablesFromCache(readCache.get(), launchables);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Reading names cache failed", e);
        }

        timer.addLeg("Dropping unnamed");
        dropUnnamed(launchables);

        timer.addLeg("Uniquifying names");
        new Uniquifier().uniquify(launchables);

        timer.addLeg("Logging name dups");
        logDuplicateNames(launchables);

        timer.addLeg("Sorting Launchables");
        try {
            DatabaseUtils.scoreLaunchables(launchables, loadLaunchHistory.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Loading launch history failed", e);
        }
        Collections.sort(launchables);

        timer.addLeg("Stabilizing Sort Order");
        launchables = StabilityUtils.stabilize(lastOrderFile, launchables);
        StabilityUtils.storeOrder(lastOrderFile, launchables);

        timer.addLeg("Updating names cache");
        updateNamesCache(nameCacheFile, launchables);

        Timber.i("loadLaunchables() timings: %s", timer);

        return launchables;
    }

    private static void logDuplicateNames(List<Launchable> launchables) {
        Map<CaseInsensitive, Integer> nameCounts = new HashMap<>();
        for (Launchable launchable: launchables) {
            if (launchable instanceof ContactLaunchable) {
                // There's nothing we can do about duplicate contact names, never mind
                continue;
            }

            Integer oldCount = nameCounts.get(launchable.getName());
            if (oldCount == null) {
                oldCount = 0;
            }
            nameCounts.put(launchable.getName(), oldCount + 1);
        }

        for (Map.Entry<CaseInsensitive, Integer> nameCount: nameCounts.entrySet()) {
            if (nameCount.getValue() == 1) {
                continue;
            }
            CaseInsensitive duplicateName = nameCount.getKey();

            // We have a dup!
            SortedSet<String> idsForName = new TreeSet<>();
            for (Launchable launchable: launchables) {
                if (duplicateName.equals(launchable.getName())) {
                    idsForName.add(launchable.getId());
                }
            }

            StringBuilder message = new StringBuilder();
            message.append("Multiple IDs share the same name: [");
            message.append(duplicateName);
            message.append("]");
            for (String id: idsForName) {
                message.append("\n-] ").append(id);
            }

            Timber.w(new RuntimeException(message.toString()));
        }
    }

    private static void dropUnnamed(List<Launchable> launchables) {
        Iterator<Launchable> iterator = launchables.iterator();
        while (iterator.hasNext()) {
            Launchable launchable = iterator.next();
            if (launchable.getName().isEmpty()) {
                iterator.remove();
            } else if (launchable.getId().equals("com.android.settings.com.samsung.android.settings.powersaving.PowerModeChangeDialogActivity")) {
                // This just unconditionally disables battery saving mode on my Galaxy S6, and it's
                // called "Battery" so having this in the list is just confusing.
                iterator.remove();
            }
        }
    }

    private static void dropDuplicateIds(List<Launchable> launchables) {
        Set<String> seenIds = new HashSet<>();
        Iterator<Launchable> iterator = launchables.iterator();
        while (iterator.hasNext()) {
            Launchable launchable = iterator.next();
            if (seenIds.contains(launchable.getId())) {
                // Should we print a warning here? All dups we have looked at have been identical,
                // so just dropping these should be fine.
                iterator.remove();
            }

            seenIds.add(launchable.getId());
        }
    }

    private static void updateNamesCache(File nameCacheFile, List<Launchable> allLaunchables) {
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
        textView.setText(launchable.getName().toString());
        ImageView imageView = view.findViewById(R.id.launchableIcon);
        new AsyncSetImageDrawable(imageView).execute(launchable);

        return view;
    }

    public void setFilter(CharSequence search) {
        if (search.length() == 0) {
            filteredLaunchables = allLaunchables;
        }

        List<Launchable> newFilteredList = new LinkedList<>();
        CaseInsensitiveQuery query = new CaseInsensitiveQuery(search.toString());
        for (Launchable launchable: allLaunchables) {
            if (launchable.matches(query)) {
                newFilteredList.add(launchable);
            }
        }

        filteredLaunchables = newFilteredList;

        notifyDataSetChanged();
    }
}
