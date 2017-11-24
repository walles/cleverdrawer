package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class StatisticsTest {
    private static Statistics createStatistics() throws IOException {
        File dbFile = File.createTempFile("testable-statistics-", ".sqlite");
        dbFile.deleteOnExit();

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        DatabaseUtils.migrate(dataSource);

        return new Statistics(dataSource);
    }

    @Test
    public void testAlphabeticFallback() throws Exception {
        Statistics testMe = createStatistics();
        Launchable ape = new Launchable("Ape");
        Launchable zebra = new Launchable("Zebra");

        Launchable[] launchables = new Launchable[]{zebra, ape};
        Arrays.sort(launchables, testMe.getComparator());
        Assert.assertThat(launchables, is(new Launchable[]{ape, zebra}));
    }

    @Test
    public void testLaunchedBetterThanNotLaunched() throws Exception {
        Statistics testMe = createStatistics();
        Launchable ape = new Launchable("Ape");
        Launchable zebra = new Launchable("Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        testMe.registerLaunch(zebra);

        Arrays.sort(launchables, testMe.getComparator());
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }

    @Test
    public void testRecentLaunchesAreBetter() throws Exception {
        Statistics testMe = createStatistics();
        Launchable ape = new Launchable("Ape");
        Launchable zebra = new Launchable("Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        testMe.registerLaunch(ape);
        Thread.sleep(1200);  // Yes, I know
        testMe.registerLaunch(zebra);

        Arrays.sort(launchables, testMe.getComparator());
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }
}
