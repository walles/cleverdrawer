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
    public void testLoadLaunchables() throws Exception {
        // Context of the app under test.
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
