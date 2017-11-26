package com.gmail.walles.johan.cleverdrawer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DatabaseUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public static void cacheNames(File file, List<Launchable> launchables) throws IOException {
        // Add all the non-null non-empty names to the cache
        Map<String, String> cache = new HashMap<>();
        for (Launchable launchable: launchables) {
            if (launchable.id == null) {
                continue;
            }
            if (launchable.getName() == null) {
                continue;
            }
            cache.put(launchable.id, launchable.getName());
        }

        // For atomicity, write to temporary file, then rename
        File tempfile = new File(file.getAbsolutePath() + ".tmp");
        objectMapper.writeValue(tempfile, cache);
        if (!tempfile.renameTo(file)) {
            throw new IOException("Updating cache file failed");
        }
    }

    public static List<Metadata> getMetadata(File file) throws IOException {
        if (!file.exists()) {
            return new LinkedList<>();
        }

        // Load the launchables table into an id->name map
        TypeReference<LinkedList<Metadata>> typeRef
                = new TypeReference<LinkedList<Metadata>>() {};
        return objectMapper.readValue(file, typeRef);
    }

    public static void saveMetadata(File file, List<Metadata> metadata) throws IOException {
        // For atomicity, write to temporary file, then rename
        File tempfile = new File(file.getAbsolutePath() + ".tmp");
        objectMapper.writeValue(tempfile, metadata);
        if (!tempfile.renameTo(file)) {
            throw new IOException("Updating cache file failed");
        }
    }
}
