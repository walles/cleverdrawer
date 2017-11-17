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
        // FIXME: Register launch in database
    }

    public Comparator<Launchable> getComparator() {
        return (o1, o2) -> {
            // FIXME: Before this fallback, sort by database content
            return o1.name.compareTo(o2.name);
        };
    }
}
