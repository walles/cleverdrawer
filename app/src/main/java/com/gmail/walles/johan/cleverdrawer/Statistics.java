package com.gmail.walles.johan.cleverdrawer;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;

import javax.sql.DataSource;

public class Statistics {
    public static Comparator<Launchable> getComparator(DataSource dataSource) throws SQLException {
        final Map<String, Double> scores = DatabaseUtils.getIdToScoreMap(dataSource);

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
}
