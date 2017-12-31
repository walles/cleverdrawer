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

public class IntentLaunchableTest {
    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testMatches() {
        Launchable launchable = new IntentLaunchable("fot", new CaseInsensitive("Gnu"));
        Assert.assertThat(launchable.contains(new CaseInsensitive("g")), is(true));
        Assert.assertThat(launchable.contains(new CaseInsensitive("n")), is(true));
        Assert.assertThat(launchable.contains(new CaseInsensitive("u")), is(true));
        Assert.assertThat(launchable.contains(new CaseInsensitive("nu")), is(true));
        Assert.assertThat(launchable.contains(new CaseInsensitive("gnu")), is(true));

        Assert.assertThat(launchable.contains(new CaseInsensitive("f")), is(false));
        Assert.assertThat(launchable.contains(new CaseInsensitive("o")), is(false));
        Assert.assertThat(launchable.contains(new CaseInsensitive("t")), is(false));
        Assert.assertThat(launchable.contains(new CaseInsensitive("fot")), is(false));
    }

    @Test
    public void testAlphabeticFallback() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new IntentLaunchable("Ape", new CaseInsensitive("Ape"));
        Launchable zebra = new IntentLaunchable("Zebra", new CaseInsensitive("Zebra"));

        List<Launchable> launchables = Arrays.asList(zebra, ape);

        DatabaseUtils.scoreLaunchables(dbFile, launchables);
        Collections.sort(launchables);

        Assert.assertThat(launchables, is(Arrays.asList(ape, zebra)));
    }

    @Test
    public void testLaunchedBetterThanNotLaunched() throws Exception {
        File dbFile = new File(tempdir.getRoot(), "testFile");
        Launchable ape = new IntentLaunchable("Ape", new CaseInsensitive("Ape"));
        Launchable zebra = new IntentLaunchable("Zebra", new CaseInsensitive("Zebra"));

        List<Launchable> launchables = Arrays.asList(ape, zebra);
        DatabaseUtils.registerLaunch(dbFile, zebra);

        DatabaseUtils.scoreLaunchables(dbFile, launchables);
        Collections.sort(launchables);

        Assert.assertThat(launchables, is(Arrays.asList(zebra, ape)));
    }
}
