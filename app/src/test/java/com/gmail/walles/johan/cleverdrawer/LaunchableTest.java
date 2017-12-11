/*
 * MIT License
 *
 * Copyright (c) 2017 Johan Walles <johan.walles@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
