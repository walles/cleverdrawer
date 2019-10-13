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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
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
    private static final int MAX_LAUNCHES = 1500;

    /**
     * Only consider the SCORING_MAX_LAUNCH_COUNT most recent launches when scoring.
     */
    public static final int SCORING_MAX_LAUNCH_COUNT = 450;

    public static Map<String, String> readIdToNameCache(File file) {
        if (!file.exists()) {
            Timber.i("No names cache file found, guessing this is the first launch");
            return Collections.emptyMap();
        }

        // Load the launchables table into an id->name map
        TypeReference<HashMap<String, String>> typeRef
                = new TypeReference<HashMap<String, String>>() {};
        try {
            return objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            Timber.w(e, "Error reading names cache, pretending it's empty");
            return Collections.emptyMap();
        }
    }

    public static void nameLaunchablesFromCache(Map<String, String> cache, List<Launchable> launchables) {
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
            throw new IOException(String.format("Updating cache file failed: '%s'->'%s'",
                    tempfile.getAbsolutePath(), file.getAbsolutePath()));
        }

        Timber.i("Caching true names took: %s", timer.toString());
    }

    public static final class LaunchMetadata {
        public String id;

        /**
         * Launch timestamp in milliseconds since epoch.
         */
        public long timestamp;

        @Override
        public String toString() {
            return "LaunchMetadata{" +
                    "id='" + id + '\'' +
                    ", timestamp=" + new Date(timestamp) +
                    '}';
        }
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

    static void scoreLaunchables(Iterable<Launchable> launchables, List<LaunchMetadata> launches) {
        if (launches.size() > SCORING_MAX_LAUNCH_COUNT) {
            launches = launches.subList(launches.size() - SCORING_MAX_LAUNCH_COUNT, launches.size());
        }

        Map<String, Double> idToScore = new HashMap<>();
        for (LaunchMetadata launch : launches) {
            Double score = idToScore.get(launch.id);
            if (score == null) {
                // Start scoring at 2, since not-scored Launchables will implicitly get 1, and we
                // want to be better than that.
                score = 2.0;
            } else {
                score++;
            }
            idToScore.put(launch.id, score);
        }

        // Apply ID scores to our launchables
        for (Launchable launchable: launchables) {
            Double score = idToScore.get(launchable.getId());
            if (score == null) {
                continue;
            }

            launchable.setScore(score);
        }
    }

    public static List<LaunchMetadata> loadLaunches(File launchHistoryFile) {
        try (InputStream launchHistoryStream = new BufferedInputStream(new FileInputStream(launchHistoryFile))) {
            return loadLaunches(launchHistoryStream);
        } catch (FileNotFoundException e) {
            return new LinkedList<>();
        } catch (IOException e) {
            Timber.w(e, "Error reading launches list, pretending it is empty");
            return new LinkedList<>();
        }
    }

    static List<LaunchMetadata> loadLaunches(InputStream launchHistoryStream) throws IOException {
        TypeReference<LinkedList<LaunchMetadata>> typeRef
                = new TypeReference<LinkedList<LaunchMetadata>>() {};
        return objectMapper.readValue(launchHistoryStream, typeRef);
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
