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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class DatabaseUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Keep track of at most this many launches.
     */
    private static final int MAX_LAUNCHES = 1000;

    /**
     * Ignore all launches older than three weeks.
     */
    private static final long ONE_WEEK_MS = 7 * 86400L * 1000L;
    private static final long MAX_LAUNCH_AGE_MS = ONE_WEEK_MS * 3L;

    public static void nameLaunchablesFromCache(File file, List<Launchable> launchables) {
        if (!file.exists()) {
            Timber.i("No names cache file found, guessing this is the first launch");
            return;
        }

        // Load the launchables table into an id->name map
        TypeReference<HashMap<String, String>> typeRef
                = new TypeReference<HashMap<String, String>>() {};
        Map<String, String> cache;
        try {
            cache = objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            Timber.w(e, "Error reading names cache, pretending it's empty");
            return;
        }

        // Update all launchable names from the map
        int updateCount = 0;
        for (Launchable launchable: launchables) {
            if (launchable.hasName()) {
                // Only name what we need; contacts already have proper names for example so let's
                // not mess those up
                continue;
            }

            String name = cache.get(launchable.getId());
            if (name == null) {
                continue;
            }
            launchable.setName(new CaseInsensitive(name));
            updateCount++;
        }

        Timber.i("%d/%d launchable names updated from cache", updateCount, launchables.size());
    }

    /**
     * Update names cache with the true Launchable names.
     * <p>
     * This method can be slow!
     */
    public static void cacheTrueNames(File file, List<Launchable> launchables) throws IOException {
        // Add all the non-null non-empty names to the cache
        Timer timer = new Timer();
        timer.addLeg("Collecting id->name map");
        Map<String, String> cache = new HashMap<>();
        for (Launchable launchable: launchables) {
            if (launchable.getId() == null) {
                continue;
            }
            CaseInsensitive name = launchable.getTrueName();
            if (name == null) {
                continue;
            }
            cache.put(launchable.getId(), name.toString());
        }

        // For atomicity, write to temporary file, then rename
        timer.addLeg("Writing to disk");
        File tempfile = new File(file.getAbsolutePath() + ".tmp");
        objectMapper.writeValue(tempfile, cache);
        if (!tempfile.renameTo(file)) {
            throw new IOException("Updating cache file failed");
        }

        Timber.i("Caching true names took: %s", timer.toString());
    }

    public static final class LaunchMetadata {
        public String id;

        /**
         * Launch timestamp in milliseconds since epoch.
         */
        public long timestamp;
    }

    /**
     * Register that this launchable has been launched.
     */
    public static void registerLaunch(File file, Launchable launchable) throws IOException {
        registerLaunch(file, launchable, System.currentTimeMillis());
    }

    /**
     * Register that this launchable has been launched.
     */
    private static void registerLaunch(File file, Launchable launchable, long timestamp)
            throws IOException
    {
        List<LaunchMetadata> launches = loadLaunches(file);

        LaunchMetadata newLaunch = new LaunchMetadata();
        newLaunch.id = launchable.getId();
        newLaunch.timestamp = timestamp;
        launches.add(newLaunch);

        // Stay below the ceiling
        while (launches.size() > MAX_LAUNCHES) {
            launches.remove(0);
        }

        saveLaunches(file, launches);
    }

    /**
     * Give all launchables a non-null score > 0
     */
    public static void scoreLaunchables(File file, Iterable<Launchable> allLaunchables) {
        Map<String, Double> idToScore = new HashMap<>();
        long now = System.currentTimeMillis();
        for (LaunchMetadata launchMetadata : loadLaunches(file)) {
            long ageMs = now - launchMetadata.timestamp;
            if (ageMs >= MAX_LAUNCH_AGE_MS) {
                continue;
            }

            Double score = idToScore.get(launchMetadata.id);
            if (score == null) {
                score = 1.0;
            } else {
                score++;
            }
            idToScore.put(launchMetadata.id, score);
        }

        for (Launchable launchable: allLaunchables) {
            Double score = idToScore.get(launchable.getId());

            // Score should be non-zero so that it can be multiplied in later stages
            if (score == null) {
                score = 1.0;
            } else {
                score += 1.0;
            }

            launchable.setScore(score);
        }
    }

    private static List<LaunchMetadata> loadLaunches(File file) {
        if (!file.exists()) {
            return new LinkedList<>();
        }

        TypeReference<LinkedList<LaunchMetadata>> typeRef
                = new TypeReference<LinkedList<LaunchMetadata>>() {};
        try {
            return objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            Timber.w(e, "Error reading launches list, pretending it is empty");
            return new LinkedList<>();
        }
    }

    private static void saveLaunches(File file, List<LaunchMetadata> metadata) throws IOException {
        // For atomicity, write to temporary file, then rename
        File tempfile = new File(file.getAbsolutePath() + ".tmp");
        objectMapper.writeValue(tempfile, metadata);
        if (!tempfile.renameTo(file)) {
            throw new IOException("Updating launch history file failed: " + file.getAbsolutePath());
        }
    }
}
