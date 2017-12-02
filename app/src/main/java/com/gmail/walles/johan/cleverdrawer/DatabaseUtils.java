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

    private static Map<String, Metadata> getIdToMetadataMap(File file) throws IOException {
        List<Metadata> stats = getMetadata(file);

        Map<String, Metadata> returnMe = new HashMap<>();
        for (Metadata stat: stats) {
            returnMe.put(stat.id, stat);
        }

        return returnMe;
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
    static void registerLaunch(File file, Launchable launchable, long timestamp) throws IOException {
        List<Metadata> metadata = getMetadata(file);
        for (Metadata candidate: metadata) {
            if (!candidate.id.equals(launchable.id)) {
                continue;
            }

            candidate.latestLaunch = timestamp;
            candidate.launchCount++;
            saveMetadata(file, metadata);
            return;
        }

        Metadata newEntry = new Metadata();
        newEntry.id = launchable.id;
        newEntry.launchCount = 1;
        newEntry.latestLaunch = timestamp;
        metadata.add(newEntry);
        saveMetadata(file, metadata);
    }

    public static void scoreLaunchables(File statsFile, Iterable<Launchable> allLaunchables) {
        Map<String, Metadata> idToMetadata;
        try {
            idToMetadata = getIdToMetadataMap(statsFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed loading statistics", e);
        }
        for (Launchable launchable: allLaunchables) {
            launchable.setScore(idToMetadata.get(launchable.id));
        }
    }

    public static final class Metadata {
        public String id;

        public int launchCount;

        /**
         * Timestamp in milliseconds since epoch.
         */
        public long latestLaunch;
    }

    public static void nameLaunchablesFromCache(File file, List<Launchable> launchables)
            throws IOException
    {
        // Load the launchables table into an id->name map
        TypeReference<HashMap<String, String>> typeRef
                = new TypeReference<HashMap<String, String>>() {};
        Map<String, String> cache = objectMapper.readValue(file, typeRef);

        // Update all launchable names from the map
        for (Launchable launchable: launchables) {
            String name = cache.get(launchable.id);
            if (name == null) {
                continue;
            }
            launchable.setName(name);
        }
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
            if (launchable.id == null) {
                continue;
            }
            String name = launchable.getTrueName();
            if (name == null) {
                continue;
            }
            cache.put(launchable.id, name);
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

    private static List<Metadata> getMetadata(File file) throws IOException {
        if (!file.exists()) {
            return new LinkedList<>();
        }

        // Load the launchables table into an id->name map
        TypeReference<LinkedList<Metadata>> typeRef
                = new TypeReference<LinkedList<Metadata>>() {};
        return objectMapper.readValue(file, typeRef);
    }

    private static void saveMetadata(File file, List<Metadata> metadata) throws IOException {
        // For atomicity, write to temporary file, then rename
        File tempfile = new File(file.getAbsolutePath() + ".tmp");
        objectMapper.writeValue(tempfile, metadata);
        if (!tempfile.renameTo(file)) {
            throw new IOException("Updating cache file failed");
        }
    }
}
