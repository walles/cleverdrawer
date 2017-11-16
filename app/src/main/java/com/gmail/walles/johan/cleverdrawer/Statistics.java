package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;
import org.sqldroid.DroidDataSource;

import java.util.Comparator;

public class Statistics {
    public Statistics(Context context) {
        DroidDataSource dataSource =
                new DroidDataSource(context.getPackageName(), "cleverness");
        ContextHolder.setContext(context);
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    /**
     * Register that this launchable has been launched.
     */
    public void update(Launchable launchable) {
        // FIXME: Register launch in database
    }

    public Comparator<Launchable> getComparator() {
        return (o1, o2) -> {
            // FIXME: Before this fallback, sort by database content
            return o1.name.compareTo(o2.name);
        };
    }
}
