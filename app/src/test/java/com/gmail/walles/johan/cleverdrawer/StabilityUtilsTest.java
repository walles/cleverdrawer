/*
 * MIT License
 *
 * Copyright (c) 2018 Johan Walles <johan.walles@gmail.com>
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
import java.util.LinkedList;
import java.util.List;

public class StabilityUtilsTest {
    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testStabilize() {
        Assert.fail("Unimplemented");
    }

    @Test
    public void testStoreOrder() {
        File lastOrder = new File(tempdir.getRoot(), "lastOrder");

        Launchable ape = new IntentLaunchable("Ape", new CaseInsensitive("Ape"));
        Launchable zebra = new IntentLaunchable("Zebra", new CaseInsensitive("Zebra"));

        List<Launchable> launchables = new LinkedList<>();
        launchables.add(ape);
        launchables.add(zebra);

        // Store the list
        StabilityUtils.storeOrder(lastOrder, launchables);

        // Load the list back
        List<String> loadedIds = StabilityUtils.loadIdOrder(lastOrder);

        List<String> expected = new LinkedList<>();
        for (Launchable launchable: launchables) {
            expected.add(launchable.getId());
        }

        Assert.assertThat(loadedIds, is(expected));
    }

    @Test
    public void testLoadOrderQuirks() {
        Assert.fail("Not implemented: Empty test and not-existing test");
    }
}
