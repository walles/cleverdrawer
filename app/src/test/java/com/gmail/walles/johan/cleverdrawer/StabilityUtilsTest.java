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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class StabilityUtilsTest {
    private static class EqualsLaunchable extends Launchable {
        public EqualsLaunchable(String id) {
            super(id);
            setName(new CaseInsensitive(id));
        }

        @Override
        @Nullable
        public Drawable getIcon() {
            return null;
        }

        @Override
        public boolean contains(CaseInsensitive substring) {
            return false;
        }

        @Override
        public double getScoreFactor() {
            return 0;
        }

        @Override
        @Nullable
        public Intent getLaunchIntent() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (!obj.getClass().equals(this.getClass())) {
                return false;
            }

            EqualsLaunchable that = (EqualsLaunchable)obj;
            return this.getId().equals(that.getId());
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }
    }

    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    private static List<Launchable> createLaunchablesWithIds(String ... ids) {
        List<Launchable> launchables = new LinkedList<>();
        for (String id: ids) {
            launchables.add(new EqualsLaunchable(id));
        }

        return Collections.unmodifiableList(launchables);
    }

    @Test
    public void testStabilizeBase() {
        // Create a list of launchables
        List<Launchable> launchables = createLaunchablesWithIds("a", "b", "c", "d", "e", "f");

        // Create a list of IDs
        List<String> oldOrderIds = Arrays.asList("a", "b", "c", "d", "e", "f");

        // Validate that the launchables list was suitably stabilized from the IDs list
        List<Launchable> stabilized = StabilityUtils.stabilize(oldOrderIds, launchables);

        List<Launchable> expected = createLaunchablesWithIds("a", "b", "c", "d", "e", "f");
        Assert.assertThat(stabilized, is(expected));
    }

    @Test
    public void testStabilizeNoOldOrder() {
        // Create a list of launchables
        List<Launchable> launchables = createLaunchablesWithIds("a", "b", "c", "d", "e", "f");

        // Create a list of IDs
        List<String> oldOrderIds = Collections.emptyList();

        // Validate that the launchables list was suitably stabilized from the IDs list
        List<Launchable> stabilized = StabilityUtils.stabilize(oldOrderIds, launchables);

        List<Launchable> expected = createLaunchablesWithIds("a", "b", "c", "d", "e", "f");
        Assert.assertThat(stabilized, is(expected));
    }

    @Test
    public void testStabilizeOneSwitch() {
        // Create a list of launchables, note a-c-b order
        List<Launchable> launchables = createLaunchablesWithIds("a", "c", "b", "d", "e", "f");

        // Create a list of IDs
        List<String> oldOrderIds = Arrays.asList("a", "b", "c", "d", "e", "f");

        // Validate that the launchables list was suitably stabilized from the IDs list
        List<Launchable> stabilized = StabilityUtils.stabilize(oldOrderIds, launchables);

        // Expect a-b-c order, stabilized from before
        List<Launchable> expected = createLaunchablesWithIds("a", "b", "c", "d", "e", "f");
        Assert.assertThat(stabilized, is(expected));
    }

    @Test
    public void testStabilizeNoNewOrder() {
        // Create a list of launchables
        List<Launchable> launchables = createLaunchablesWithIds();

        // Create a list of IDs
        List<String> oldOrderIds = Arrays.asList("a", "b", "c", "d", "e", "f");

        // Validate that the launchables list was suitably stabilized from the IDs list
        List<Launchable> stabilized = StabilityUtils.stabilize(oldOrderIds, launchables);

        List<Launchable> expected = createLaunchablesWithIds();
        Assert.assertThat(stabilized, is(expected));
    }

    @Test
    public void testStabilizeCrossBoundarySwitch() {
        // Create a list of launchables, note -e-d- order
        List<Launchable> launchables = createLaunchablesWithIds("a", "b", "c", "e", "d", "f");

        // Create a list of IDs
        List<String> oldOrderIds = Arrays.asList("a", "b", "c", "d", "e", "f");

        // Validate that the launchables list was suitably stabilized from the IDs list
        List<Launchable> stabilized = StabilityUtils.stabilize(oldOrderIds, launchables);

        List<Launchable> expected = createLaunchablesWithIds("a", "b", "c", "e", "d", "f");
        Assert.assertThat(stabilized, is(expected));
    }

    @Test
    public void testStabilizeOneNewAtStart() {
        // Create a list of launchables
        List<Launchable> launchables = createLaunchablesWithIds("x", "a", "b", "c", "d", "e", "f");

        // Create a list of IDs
        List<String> oldOrderIds = Arrays.asList("a", "b", "c", "d", "e", "f");

        // Validate that the launchables list was suitably stabilized from the IDs list
        List<Launchable> stabilized = StabilityUtils.stabilize(oldOrderIds, launchables);

        List<Launchable> expected = createLaunchablesWithIds("a", "b", "c", "x", "d", "e", "f");
        Assert.assertThat(stabilized, is(expected));
    }

    private void testStoreOrder(File lastOrder) {
        List<Launchable> launchables = createLaunchablesWithIds("Ape", "Zebra");

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
    public void testStoreOrderBasic() {
        testStoreOrder(new File(tempdir.getRoot(), "lastOrder"));
    }

    @Test
    public void testStoreOrderOverwrite() throws Exception {
        File lastOrder = new File(tempdir.getRoot(), "lastOrder");

        try (PrintWriter out = new PrintWriter(lastOrder)) {
            out.println("This contents should be overwritten by the test");
        }

        testStoreOrder(lastOrder);
    }

    @Test
    public void testLoadIdOrderNoFile() {
        File doesntExist = new File(tempdir.getRoot(), "doesntExist");

        Assert.assertThat(StabilityUtils.loadIdOrder(doesntExist), is(Collections.emptyList()));
    }
}
