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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class LaunchableAdapterTest {
    @Test
    public void testLoadLaunchablesNoDuplicateIds() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        List<Launchable> launchables = LaunchableAdapter.loadLaunchables(appContext);

        // Map all IDs to the launchables with that ID
        HashMap<String, List<Launchable>> idToLaunchables = new HashMap<>();
        for (Launchable launchable: launchables) {
            List<Launchable> launchablesForId = idToLaunchables.get(launchable.id);
            if (launchablesForId == null) {
                launchablesForId = new LinkedList<>();
                idToLaunchables.put(launchable.id, launchablesForId);
            }
            launchablesForId.add(launchable);
        }

        boolean oneLaunchablePerId = true;
        StringBuilder problem = new StringBuilder();
        for (Map.Entry<String, List<Launchable>> idWithLaunchables: idToLaunchables.entrySet()) {
            if (idWithLaunchables.getValue().size() == 1) {
                continue;
            }

            oneLaunchablePerId = false;
            problem.append('\n');
            problem.append(idWithLaunchables.getKey()).append('\n');
            for (Launchable launchable: idWithLaunchables.getValue()) {
                problem.append("-- ").append(launchable.getName()).append('\n');
            }
        }

        Assert.assertThat(problem.toString(), oneLaunchablePerId, is(true));
    }
}
