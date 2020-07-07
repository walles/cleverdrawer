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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;

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
        // 100.0 = All launches were done from the same place as previously, muscle memory heaven!
        final double BASELINE_SCORE_PER_LAUNCH = 71.727;

        Collection<SimulatedLaunch> simulatedLaunches = simulateLaunches(loadLaunchesFromFile());
        Assert.assertThat("Not enough data for both warming up and measuring",
                simulatedLaunches.size(), Matchers.greaterThan(DatabaseUtils.SCORING_MAX_LAUNCH_COUNT * 2));

        int score = 0;
        int scoreCount = 0;
        int warmupSkips = DatabaseUtils.SCORING_MAX_LAUNCH_COUNT;
        Map<String, Integer> lastLaunchIndices = new HashMap<>();
        for (SimulatedLaunch simulatedLaunch: simulatedLaunches) {
            int launchIndex = simulatedLaunch.getLaunchIndex();
            Integer lastLaunchIndex = lastLaunchIndices.get(simulatedLaunch.launch.id);
            lastLaunchIndices.put(simulatedLaunch.launch.id, launchIndex);

            if (warmupSkips-- > 0) {
                continue;
            }

            scoreCount++;

            if (launchIndex >= 12) {
                // Too far down, too hard to find, no points
                continue;
            }

            // Findable by looking for it
            score += 50;

            if (lastLaunchIndex == null) {
                // No previous launch, no muscle memory bonus
                continue;
            }

            if (lastLaunchIndex != launchIndex) {
                // Not the same as the previous launch, no muscle memory bonus
                continue;
            }

            score += 50;
        }

        // We do an exact match; if something changes the score we should investigate and either
        // update our baseline or fix whatever we broke.
        Assert.assertThat(score / (double)scoreCount,
                closeTo(BASELINE_SCORE_PER_LAUNCH, 0.1));
    }

    private String findMostCommonId(List<DatabaseUtils.LaunchMetadata> launches) {
        Map<String, Integer> id2score = new HashMap<>();
        for (DatabaseUtils.LaunchMetadata launch: launches) {
            int score = id2score.getOrDefault(launch.id, 0);
            id2score.put(launch.id, score + 1);
        }

        // Find the highest scored ID
        String highestScoredId = null;
        int highestScore = 0;

        for (Map.Entry<String, Integer> entry: id2score.entrySet()) {
            if (entry.getValue() <= highestScore) {
                continue;
            }

            highestScore = entry.getValue();
            highestScoredId = entry.getKey();
        }
        assert highestScoredId != null;

        return highestScoredId;
    }

    /**
     * Verify how well we manage to respond to changes in user behavior.
     */
    @Test
    public void testScoreLaunchablesResponsiveness() throws IOException {
        // We should be at least this good, otherwise we have regressed. If you update the example
        // data that we're testing, this baseline will need to change.
        //
        // This is the number of launches required until the new favorite gets to the top, lower is
        // better.
        final int LAUNCHES_TO_TOP_BASELINE = 31;

        List<DatabaseUtils.LaunchMetadata> launches = loadLaunchesFromFile();
        String replaceId = findMostCommonId(launches);
        String replacementId = "JOHAN";

        // Change user behavior right after the warmup run
        int warmupSkips = DatabaseUtils.SCORING_MAX_LAUNCH_COUNT;
        int count = 0;
        for (DatabaseUtils.LaunchMetadata launch: launches) {
            if (count++ < warmupSkips) {
                continue;
            }

            if (!launch.id.equals(replaceId)) {
                continue;
            }

            // Post-warmup most common ID, this is what we want to behave well for responsiveness
            // to be high
            launch.id = replacementId;
        }

        Collection<SimulatedLaunch> simulatedLaunches = simulateLaunches(launches);
        Assert.assertThat("Not enough data for both warming up and measuring",
                simulatedLaunches.size(), Matchers.greaterThan(DatabaseUtils.SCORING_MAX_LAUNCH_COUNT * 2));

        int launchesToTop = 0;
        Map<String, Integer> lastLaunchIndices = new HashMap<>();
        for (SimulatedLaunch simulatedLaunch: simulatedLaunches) {
            int launchIndex = simulatedLaunch.getLaunchIndex();
            Integer lastLaunchIndex = lastLaunchIndices.get(simulatedLaunch.launch.id);
            lastLaunchIndices.put(simulatedLaunch.launch.id, launchIndex);

            if (warmupSkips-- > 0) {
                continue;
            }

            if (!replacementId.equals(simulatedLaunch.launch.id)) {
                // We measure the performance of replacementId only
                continue;
            }

            launchesToTop++;
            if (launchIndex >= 4) {
                // Not yet on the first line, never mind
                continue;
            }

            if (lastLaunchIndex == null) {
                // No previous launch, not there yet
                continue;
            }

            if (lastLaunchIndex != launchIndex) {
                // Not the same as the previous launch, not there yet
                continue;
            }

            // Done!
            Assert.assertThat(launchesToTop, is(LAUNCHES_TO_TOP_BASELINE));
            return;
        }

        Assert.fail("The new favorite app never reached the top");
    }

    private static class SimulatedLaunch {
        protected List<Launchable> launchables;
        protected DatabaseUtils.LaunchMetadata launch;

        /** Locate what we're launching in the sorted launchables list */
        public int getLaunchIndex() {
            for (int i = 0; i < launchables.size(); i++) {
                Launchable launchable = launchables.get(i);
                if (launch.id.equals(launchable.getId())) {
                    return i;
                }
            }

            return launchables.size();
        }
    }

    private List<DatabaseUtils.LaunchMetadata> loadLaunchesFromFile() throws IOException {
        List<DatabaseUtils.LaunchMetadata> launchHistory;
        try (InputStream launchHistoryStream =
                     getClass().getClassLoader().getResourceAsStream("real-statistics-example.json"))
        {
            launchHistory = DatabaseUtils.loadLaunches(launchHistoryStream);
        }
        Assert.assertThat(launchHistory, is(not(empty())));

        return launchHistory;
    }

    private Collection<SimulatedLaunch> simulateLaunches(List<DatabaseUtils.LaunchMetadata> launchHistory) {
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
                public boolean matches(CaseInsensitiveQuery query) {
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

                @Nullable
                @Override
                public Intent getAppInfoIntent() {
                    return null;
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
