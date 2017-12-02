package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LaunchableTest {
    private static final long ONE_WEEK_MS = 7 * 86400L * 1000L;

    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testMatches() {
        Launchable launchable = new Launchable("fot", "Gnu");
        Assert.assertThat(launchable.matches("g"), is(true));
        Assert.assertThat(launchable.matches("n"), is(true));
        Assert.assertThat(launchable.matches("u"), is(true));
        Assert.assertThat(launchable.matches("nu"), is(true));
        Assert.assertThat(launchable.matches("gnu"), is(true));

        Assert.assertThat(launchable.matches("f"), is(false));
        Assert.assertThat(launchable.matches("o"), is(false));
        Assert.assertThat(launchable.matches("t"), is(false));
        Assert.assertThat(launchable.matches("fot"), is(false));
    }

    @Test
    public void testAlphabeticFallback() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        List<Launchable> launchables = Arrays.asList(zebra, ape);

        DatabaseUtils.scoreLaunchables(dbFile, launchables);
        Collections.sort(launchables);

        Assert.assertThat(launchables, is(Arrays.asList(ape, zebra)));
    }

    @Test
    public void testLaunchedBetterThanNotLaunched() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        List<Launchable> launchables = Arrays.asList(ape, zebra);
        DatabaseUtils.registerLaunch(dbFile, zebra);

        DatabaseUtils.scoreLaunchables(dbFile, launchables);
        Collections.sort(launchables);

        Assert.assertThat(launchables, is(Arrays.asList(zebra, ape)));
    }

    @Test
    public void testRecentLaunchesAreBetter() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new Launchable("Ape", "Ape");
        Launchable zebra = new Launchable("Zebra", "Zebra");

        List<Launchable> launchables = Arrays.asList(ape, zebra);
        DatabaseUtils.registerLaunch(dbFile,   ape, System.currentTimeMillis() - 3 * ONE_WEEK_MS);
        DatabaseUtils.registerLaunch(dbFile, zebra, System.currentTimeMillis() - 2 * ONE_WEEK_MS);

        DatabaseUtils.scoreLaunchables(dbFile, launchables);
        Collections.sort(launchables);

        Assert.assertThat(launchables, is(Arrays.asList(zebra, ape)));
    }
}
