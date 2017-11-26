package com.gmail.walles.johan.cleverdrawer;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

public class DatabaseUtilsTest {
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testNameCaching() throws Exception {
        // Create an empty database
        File dbFile = new File(tempdir.getRoot(), "testFile");

        // Populate cache with some mappings
        Launchable l1 = new Launchable("id: 1", "name: One");
        Launchable l2 = new Launchable("id: 2", "name: Two");
        DatabaseUtils.cacheNames(dbFile, Arrays.asList(l1, l2));

        // Populate some new launchables with those mappings, from a new data source to simulate
        // app restart
        l1 = new Launchable("id: 1", null);
        l2 = new Launchable("id: 2", null);
        DatabaseUtils.nameLaunchablesFromCache(dbFile, Arrays.asList(l1, l2));

        // Verify that the new launchables got the right names
        Assert.assertThat(l1.getName(), is("name: One"));
        Assert.assertThat(l2.getName(), is("name: Two"));
    }
}
