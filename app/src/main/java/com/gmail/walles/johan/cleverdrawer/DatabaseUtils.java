package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;

import com.jcabi.jdbc.JdbcSession;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;
import org.sqldroid.DroidDataSource;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

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
    {
        // FIXME: Load the launchables table into an id->name map

        // FIXME: Update all launchable names from the map

    }

    public static void cacheNames(DataSource dataSource, List<Launchable> launchables)
            throws SQLException
    {
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
    }
}
