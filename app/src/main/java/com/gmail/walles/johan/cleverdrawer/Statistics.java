package com.gmail.walles.johan.cleverdrawer;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.SingleOutcome;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

public class Statistics {
    // This should really be final
    private final DataSource dataSource;

    public Statistics(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Register that this launchable has been launched.
     */
    public void registerLaunch(Launchable launchable) throws SQLException {
        boolean rowExists = 0 < new JdbcSession(dataSource).
                sql("SELECT COUNT(*) FROM statistics WHERE id = ?").
                set(launchable.id).
                select(new SingleOutcome<>(Long.class));

        long now = System.currentTimeMillis() / 1000L;

        // If the launchable already has a row...
        if (rowExists) {
            // ... then update the stats on that row.
            new JdbcSession(dataSource).
                    sql("UPDATE statistics SET launch_count = launch_count + 1, latest_launch = ? WHERE id = ?").
                    set(launchable.id).
                    execute();
        } else {
            // No row exists, create a new one
            new JdbcSession(dataSource).
                    sql("INSERT INTO statistics (id, launch_count, first_launch, latest_launch) VALUES (?, ?, ?, ?)").
                    set(launchable.id).
                    set(1).
                    set(now).
                    set(now).
                    execute();
        }
    }

    public Comparator<Launchable> getComparator() throws SQLException {
        final Map<String, Double> scores = getIdToScoreMap();

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

    private Map<String, Double> getIdToScoreMap() throws SQLException {
        final long nowSeconds = System.currentTimeMillis() / 1000L;
        return new JdbcSession(dataSource).
                sql("SELECT id, launch_count, latest_launch FROM statistics").
                select((rset, stmt) -> {
                    Map<String, Double> returnMe = new HashMap<>();
                    for (rset.next(); !rset.isAfterLast(); rset.next()) {
                        String id = rset.getString(1);
                        int launch_count = rset.getInt(2);
                        long latest_launch = rset.getLong(3);

                        double score = launch_count / (double)(nowSeconds - latest_launch);
                        returnMe.put(id, score);
                    }
                    return returnMe;
                });
    }
}
