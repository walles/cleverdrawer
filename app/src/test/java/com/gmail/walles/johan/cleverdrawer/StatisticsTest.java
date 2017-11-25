package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

public class StatisticsTest {
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testAlphabeticFallback() throws Exception {
        Statistics testMe =
                new Statistics(TestUtils.getMigratedFileDataSource(tempdir.newFile()));
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{zebra, ape};
        Arrays.sort(launchables, testMe.getComparator());
        Assert.assertThat(launchables, is(new Launchable[]{ape, zebra}));
    }

    @Test
    public void testLaunchedBetterThanNotLaunched() throws Exception {
        Statistics testMe =
                new Statistics(TestUtils.getMigratedFileDataSource(tempdir.newFile()));
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        testMe.registerLaunch(zebra);

        Arrays.sort(launchables, testMe.getComparator());
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }

    @Test
    public void testRecentLaunchesAreBetter() throws Exception {
        Statistics testMe =
                new Statistics(TestUtils.getMigratedFileDataSource(tempdir.newFile()));
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        testMe.registerLaunch(ape);
        Thread.sleep(1200);  // Yes, I know
        testMe.registerLaunch(zebra);

        Arrays.sort(launchables, testMe.getComparator());
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }
}
