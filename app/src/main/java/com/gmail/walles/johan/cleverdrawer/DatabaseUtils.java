package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.SingleOutcome;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;
import org.sqldroid.DroidDataSource;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import timber.log.Timber;

public class DatabaseUtils {
    public static void migrate(DataSource dataSource) {
        Timer timer = new Timer();
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

        timer.addLeg("Migrating db");
        flyway.migrate();

        Timber.i("Statistics timings: %s", timer);
    }

    public static DataSource getMigratedAndroidDataSource(Context context) {
        ContextHolder.setContext(context);
        final DataSource dataSource =
                new DroidDataSource(context.getPackageName(), "cleverness");

        DatabaseUtils.migrate(dataSource);
        return dataSource;
    }

    public static void nameLaunchablesFromCache(
            DataSource dataSource, List<Launchable> launchables)
            throws SQLException
    {
        // Load the launchables table into an id->name map
        Map<String, String> cache = new HashMap<>();
        new JdbcSession(dataSource).
                sql("SELECT id, name FROM names_cache").
                select((rset, stmt) -> {
                    for (rset.next(); !rset.isAfterLast(); rset.next()) {
                        String id = rset.getString(1);
                        String name = rset.getString(2);

                        cache.put(id, name);
                    }
                    return cache;
                });

        // Update all launchable names from the map
        for (Launchable launchable: launchables) {
            String name = cache.get(launchable.id);
            if (name == null) {
                continue;
            }
            launchable.setName(name);
        }
    }

    public static void cacheNames(DataSource dataSource, List<Launchable> launchables)
            throws SQLException
    {
        Timer timer = new Timer();
        SQLException error = null;

        JdbcSession session = new JdbcSession(dataSource);
        try {
            // Start a transaction
            session.autocommit(false);

            // Empty the name cache table
            session
                    .sql("DELETE FROM names_cache")
                    .execute();

            // Add all the non-null non-empty names to the cache
            for (Launchable launchable: launchables) {
                if (launchable.id == null) {
                    continue;
                }
                if (launchable.getName() == null) {
                    continue;
                }
                session
                        .sql("INSERT INTO names_cache (id, name) VALUES (?, ?)")
                        .set(launchable.id)
                        .set(launchable.getName())
                        .execute();
            }
        } catch (SQLException e) {
            error = e;
        } finally {
            if (error == null) {
                session.commit();
            } else {
                try {
                    session
                            .sql("ROLLBACK TRANSACTION")
                            .execute();
                } catch (SQLException e) {
                    Timber.e(e, "Cache update rollback failed");
                }
                throw error;
            }
        }

        Timber.i("Caching names took: %s", timer.toString());
    }

    public static Map<String, Double> getIdToScoreMap(DataSource dataSource) throws SQLException {
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

    /**
     * Register that this launchable has been launched.
     */
    public static void registerLaunch(DataSource dataSource, Launchable launchable) throws SQLException {
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
}
