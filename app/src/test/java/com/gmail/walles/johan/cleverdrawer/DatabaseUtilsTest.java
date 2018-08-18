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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.empty;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DatabaseUtilsTest {
    @SuppressWarnings("CanBeFinal")
    @Rule
    public TemporaryFolder tempdir = new TemporaryFolder();

    @Test
    public void testNameCaching() throws Exception {
        // Create an empty database
        File dbFile = new File(tempdir.getRoot(), "testFile");

        // Populate cache with some mappings
        Launchable l1 = new IntentLaunchable("id: 1", new CaseInsensitive("name: One"));
        Launchable l2 = new IntentLaunchable("id: 2", new CaseInsensitive("name: Two"));
        DatabaseUtils.cacheTrueNames(dbFile, Arrays.asList(l1, l2));

        // Populate some new launchables with those mappings, from a new data source to simulate
        // app restart
        l1 = new IntentLaunchable("id: 1", null);
        l2 = new IntentLaunchable("id: 2", null);
        Map<String, String> cache = DatabaseUtils.readIdToNameCache(dbFile);
        DatabaseUtils.nameLaunchablesFromCache(cache, Arrays.asList(l1, l2));

        // Verify that the new launchables got the right names
        Assert.assertThat(l1.getName(), is(new CaseInsensitive("name: One")));
        Assert.assertThat(l2.getName(), is(new CaseInsensitive("name: Two")));
    }

    @Test
    public void testScoreLaunchablesOnlyScoreLaunched() {
        DummyLaunchable interesting = new DummyLaunchable("interesting");
        DummyLaunchable boring = new DummyLaunchable("boring");

        List<Launchable> launchables = new LinkedList<>();
        launchables.add(interesting);
        launchables.add(boring);

        DatabaseUtils.LaunchMetadata launch = new DatabaseUtils.LaunchMetadata();
        launch.timestamp = 1234;
        launch.id = "interesting";

        List<DatabaseUtils.LaunchMetadata> launches = new LinkedList<>();
        launches.add(launch);

        DatabaseUtils.scoreLaunchables(launchables, launches);

        Assert.assertThat(interesting.hasScore(), is(true));
        Assert.assertThat(boring.hasScore(), is(false));
    }

    /**
     * Verify how well we manage to predict what the user is going to launch next.
     */
    @Test
    public void testScoreLaunchablesPerformance() throws IOException {
        // We should be at least this good, otherwise we have regressed. If you update the example
        // data that we're testing, this baseline will need to change.
        //
        // 100.0 = All launches were done from one of the first four launchables
        final double BASELINE_SCORE_PER_LAUNCH = 58.76;

        int score = 0;
        for (SimulatedLaunch simulatedLaunch: simulatedLaunches()) {
            // Locate what we want to launch in the sorted launchables list
            int index = -1;
            for (int i = 0; i < simulatedLaunch.launchables.size(); i++) {
                Launchable launchable = simulatedLaunch.launchables.get(i);
                if (simulatedLaunch.launch.id.equals(launchable.getId())) {
                    index = i;
                }
            }

            // Rate how well this launch was predicted
            if (index < 4) {
                score += 100;
            } else if (index < 8) {
                score += 50;
            } else {
                // Further down than that isn't good enough
            }
        }

        // We do an exact match; if something changes the score we should investigate and either
        // update our baseline or fix whatever we broke.
        Assert.assertThat(score / (double)getLaunchHistorySize(),
                closeTo(BASELINE_SCORE_PER_LAUNCH, 0.1));
    }

    /**
     * Verify that we don't move the top of the list around too much; it's bad for muscle memory.
     */
    @Test
    public void testScoreLaunchablesJumpiness() throws IOException {
        // We should be at most this bad, otherwise we have regressed. If you update the example
        // data that we're testing, this baseline will need to change.
        //
        // 0 = Nothing ever moved at the top of the list
        final double BASELINE_JUMPS_PER_LAUNCH = 0.173;

        int jumpiness = 0;
        List<Launchable> previousLaunchables = null;
        for (SimulatedLaunch simulatedLaunch: simulatedLaunches()) {
            if (previousLaunchables != null) {
                for (int i = 0; i < 8; i++) {
                    if (i >= previousLaunchables.size()) {
                        break;
                    }
                    if (i >= simulatedLaunch.launchables.size()) {
                        break;
                    }

                    Launchable previousLaunchable = previousLaunchables.get(i);
                    Launchable currentLaunchable = simulatedLaunch.launchables.get(i);
                    if (!Objects.equals(
                            previousLaunchable.getId(),
                            currentLaunchable.getId()))
                    {
                        if (previousLaunchable.hasScore()) {
                            // Jumpiness is only relevant when something we care about is moved
                            jumpiness++;
                        }
                    }
                }
            }

            previousLaunchables = simulatedLaunch.launchables;
        }

        // We do an exact match; if something changes the score we should investigate and either
        // update our baseline or fix whatever we broke.
        Assert.assertThat(jumpiness / (double)getLaunchHistorySize(),
                closeTo(BASELINE_JUMPS_PER_LAUNCH, 0.1));
    }

    private static class SimulatedLaunch {
        public List<Launchable> launchables;
        public DatabaseUtils.LaunchMetadata launch;
    }

    private Iterable<SimulatedLaunch> simulatedLaunches() throws IOException {
        List<DatabaseUtils.LaunchMetadata> launchHistory;
        try (InputStream launchHistoryStream =
                     getClass().getClassLoader().getResourceAsStream("real-statistics-example.json"))
        {
            launchHistory = DatabaseUtils.loadLaunches(launchHistoryStream);
        }
        Assert.assertThat(launchHistory, is(not(empty())));

        List<SimulatedLaunch> returnMe = new ArrayList<>();
        List<String> lastOrder = Collections.emptyList();
        for (DatabaseUtils.LaunchMetadata launch: launchHistory) {
            // Replay launch history until before our current launch
            long now = launch.timestamp;
            List<DatabaseUtils.LaunchMetadata> previousLaunches =
                    beforeTimestamp(launchHistory, now);
            List<Launchable> launchables = listLaunchables(previousLaunches);

            DatabaseUtils.scoreLaunchables(launchables, previousLaunches);
            Collections.sort(launchables);
            launchables = StabilityUtils.stabilize(lastOrder, launchables);
            lastOrder = extractIds(launchables);

            SimulatedLaunch simulatedLaunch = new SimulatedLaunch();
            simulatedLaunch.launchables = launchables;
            simulatedLaunch.launch = launch;
            returnMe.add(simulatedLaunch);
        }

        return returnMe;
    }

    private List<String> extractIds(List<Launchable> launchables) {
        List<String> ids = new LinkedList<>();
        for (Launchable launchable: launchables) {
            ids.add(launchable.getId());
        }
        return ids;
    }

    private int getLaunchHistorySize() throws IOException {
        try (InputStream launchHistoryStream =
                     getClass().getClassLoader().getResourceAsStream("real-statistics-example.json"))
        {
            return DatabaseUtils.loadLaunches(launchHistoryStream).size();
        }
    }

    /**
     * Create a list of launchables from the IDs in the launch metadata.
     */
    private List<Launchable> listLaunchables(List<DatabaseUtils.LaunchMetadata> launches) {
        Map<String, Launchable> idToLaunchable = new HashMap<>();
        for (DatabaseUtils.LaunchMetadata launch: launches) {
            if (idToLaunchable.containsKey(launch.id)) {
                continue;
            }

            Launchable launchable = new Launchable(launch.id) {
                @Override
                public Drawable getIcon() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean contains(CaseInsensitive substring) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public double getScoreFactor() {
                    return 1.0;
                }

                @Override
                public Intent getLaunchIntent() {
                    throw new UnsupportedOperationException();
                }
            };
            launchable.setName(new CaseInsensitive(launch.id));
            idToLaunchable.put(launch.id, launchable);
        }

        return new ArrayList<>(idToLaunchable.values());
    }

    /**
     * Create a new list containing only launches before the given timestamp.
     */
    private List<DatabaseUtils.LaunchMetadata> beforeTimestamp(
            List<DatabaseUtils.LaunchMetadata> launchHistory, long now)
    {
        int firstAfterNowIndex = launchHistory.size();
        for (int i = 0; i < launchHistory.size(); i++) {
            DatabaseUtils.LaunchMetadata launch = launchHistory.get(i);
            if (launch.timestamp >= now) {
                firstAfterNowIndex = i;
                break;
            }
        }

        return launchHistory.subList(0, firstAfterNowIndex);
    }
}
