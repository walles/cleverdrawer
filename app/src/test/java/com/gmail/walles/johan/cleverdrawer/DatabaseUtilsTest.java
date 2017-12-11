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
        DatabaseUtils.cacheTrueNames(dbFile, Arrays.asList(l1, l2));

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
