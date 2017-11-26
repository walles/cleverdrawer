package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

import javax.sql.DataSource;

public class StatisticsTest {
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testAlphabeticFallback() throws Exception {
        DataSource dataSource = TestUtils.getMigratedFileDataSource(tempdir.newFile());
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{zebra, ape};
        Arrays.sort(launchables, Statistics.getComparator(dataSource));
        Assert.assertThat(launchables, is(new Launchable[]{ape, zebra}));
    }

    @Test
    public void testLaunchedBetterThanNotLaunched() throws Exception {
        DataSource dataSource = TestUtils.getMigratedFileDataSource(tempdir.newFile());
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        DatabaseUtils.registerLaunch(dataSource, zebra);

        Arrays.sort(launchables, Statistics.getComparator(dataSource));
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }

    @Test
    public void testRecentLaunchesAreBetter() throws Exception {
        DataSource dataSource = TestUtils.getMigratedFileDataSource(tempdir.newFile());
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        Launchable[] launchables = new Launchable[]{ape, zebra};
        DatabaseUtils.registerLaunch(dataSource, ape);
        Thread.sleep(1200);  // Yes, I know
        DatabaseUtils.registerLaunch(dataSource, zebra);

        Arrays.sort(launchables, Statistics.getComparator(dataSource));
        Assert.assertThat(launchables, is(new Launchable[]{zebra, ape}));
    }
}
