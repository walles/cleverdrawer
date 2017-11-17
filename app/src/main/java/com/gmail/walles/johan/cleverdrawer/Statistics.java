package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.SingleOutcome;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;
import org.jetbrains.annotations.TestOnly;
import org.sqldroid.DroidDataSource;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

public class Statistics {
    // This should really be final
    private DataSource dataSource;

    public Statistics(Context context) {
        ContextHolder.setContext(context);
        setDataSource(new DroidDataSource(context.getPackageName(), "cleverness"));
    }

    @TestOnly
    Statistics() {
        // This constructor intentionally left blank
    }

    void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);

        // Tell the unit tests where to find our migrations. To get this to work you must make sure
        // this environment variable is set when you launch the tests.
        String migrationsPath = System.getenv("FLYWAY_MIGRATIONS_PATH");
        if (migrationsPath != null) {
            if (!new File(migrationsPath).exists()) {
                throw new RuntimeException("FLYWAY_MIGRATIONS_PATH set to non-existing path: <" + migrationsPath + ">");
            }
            flyway.setLocations("filesystem:" + migrationsPath);
        }

        flyway.migrate();
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

            // Note reverse order comparison to get the highest scores first
            int result = Double.compare(score2, score1);
            if (result != 0) {
                return result;
            }

            // Don't know, go for alphabetic order
            return o1.name.compareTo(o2.name);
        };
    }

    private Map<String, Double> getIdToScoreMap() throws SQLException {
        final long nowSeconds = System.currentTimeMillis() / 1000L;
        return new JdbcSession(dataSource).
                sql("SELECT id, launch_count, latest_launch FROM statistics").
                select((rset, stmt) -> {
                    Map<String, Double> returnMe = new HashMap<>();
                    for (/* rset.first() */; !rset.isAfterLast(); rset.next()) {

                        String id = rset.getString(1);
                        int launch_count = rset.getInt(2);
                        long latest_launch = rset.getLong(3);

                        double score = launch_count * (double)(nowSeconds - latest_launch);
                        returnMe.put(id, score);
                    }
                    return returnMe;
                });
    }
}
