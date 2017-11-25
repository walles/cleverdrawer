package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;
import org.sqldroid.DroidDataSource;

import java.io.File;
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

    public static void cacheNames(DataSource dataSource, List<Launchable> launchables) {
        // FIXME: Start a transaction

        // FIXME: Empty the name cache table

        // FIXME: Add all the non-null non-empty names to the cache

        // FIXME: Close the transaction

    }
}
