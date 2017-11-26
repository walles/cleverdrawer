package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

public class StatisticsTest {
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testAlphabeticFallback() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{zebra, ape};
        Arrays.sort(launchables, Statistics.getComparator(dbFile));
        Assert.assertThat(launchables, is(new Launchable[]{ape, zebra}));
    }

    @Test
    public void testLaunchedBetterThanNotLaunched() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        Statistics.registerLaunch(dbFile, zebra);

        Arrays.sort(launchables, Statistics.getComparator(dbFile));
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }

    @Test
    public void testRecentLaunchesAreBetter() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        Statistics.registerLaunch(dbFile, ape);
        Thread.sleep(1200);  // Yes, I know
        Statistics.registerLaunch(dbFile, zebra);

        Arrays.sort(launchables, Statistics.getComparator(dbFile));
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }
}
