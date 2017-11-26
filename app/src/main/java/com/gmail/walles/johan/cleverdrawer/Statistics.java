package com.gmail.walles.johan.cleverdrawer;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistics {
    public static Comparator<Launchable> getComparator(File file) throws IOException {
        final Map<String, Double> scores = getIdToScoreMap(file);

        return (o1, o2) -> {
            Double score1 = scores.get(o1.id);
            if (score1 == null) {
                score1 = 0d;
            }

            Double score2 = scores.get(o2.id);
            if (score2 == null) {
                score2 = 0d;
            }

            // Note reverse order to get higher scores at the top
            int result = Double.compare(score2, score1);
            if (result != 0) {
                return result;
            }

            // Don't know, go for alphabetic order
            return o1.getName().compareTo(o2.getName());
        };
    }

    /**
     * Register that this launchable has been launched.
     */
    public static void registerLaunch(File file, Launchable launchable) throws IOException {
        List<DatabaseUtils.Metadata> metadata = DatabaseUtils.getMetadata(file);
        for (DatabaseUtils.Metadata candidate: metadata) {
            if (!candidate.id.equals(launchable.id)) {
                continue;
            }

            candidate.latestLaunch = System.currentTimeMillis();
            candidate.launchCount++;
            DatabaseUtils.saveMetadata(file, metadata);
            return;
        }

        DatabaseUtils.Metadata newEntry = new DatabaseUtils.Metadata();
        newEntry.id = launchable.id;
        newEntry.launchCount = 1;
        newEntry.latestLaunch = System.currentTimeMillis();
        metadata.add(newEntry);
        DatabaseUtils.saveMetadata(file, metadata);
    }

    private static Map<String, Double> getIdToScoreMap(File file) throws IOException {
        final long now = System.currentTimeMillis();
        List<DatabaseUtils.Metadata> stats = DatabaseUtils.getMetadata(file);

        Map<String, Double> returnMe = new HashMap<>();
        for (DatabaseUtils.Metadata stat: stats) {
            double score = stat.launchCount / (double)(now - stat.latestLaunch);
            returnMe.put(stat.id, score);
        }

        return returnMe;
    }
}
