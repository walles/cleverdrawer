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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UniquifierTest {
    private static final Random RANDOM = new Random();
    private static final Pattern DECORATED = Pattern.compile(".*\\((.*)\\)");

    private void testUniquify(String ... args) {
        final String MARKER = "x";

        if (args.length %2 != 0) {
            throw new IllegalArgumentException("Arguments must be paired!");
        }

        List<Launchable> launchables = new LinkedList<>();
        List<String> expectedUniquifiers = new LinkedList<>();
        int index = 0;
        while (index < args.length) {
            String packageName = args[index];
            String uniquifier = args[index + 1];
            index += 2;

            Launchable launchable = new DummyLaunchable(packageName);
            launchable.setName(new CaseInsensitive(""));
            launchables.add(launchable);

            expectedUniquifiers.add(uniquifier);
        }

        // Add a random element to prevent the test from being too simplistic
        launchables.add(RANDOM.nextInt(launchables.size()), new DummyLaunchable(MARKER));

        new Uniquifier().uniquify(launchables);
        List<String> actualUniquifiers = new LinkedList<>();
        for (Launchable launchable: launchables) {
            String name = launchable.getName().toString();
            if (MARKER.equals(name)) {
                continue;
            }

            String uniquifier = null;  // Not decorated
            Matcher matcher = DECORATED.matcher(name);
            if (matcher.matches()) {
                uniquifier = matcher.group(1);
                Assert.assertThat(uniquifier, not(is("")));
            }
            actualUniquifiers.add(uniquifier);
        }

        Assert.assertThat(actualUniquifiers, is(expectedUniquifiers));
    }

    @Test
    public void shouldNotRegress() {
        testUniquify(
                "com.android.settings.com.android.settings.Settings$AppNotificationSettingsActivity", "App",
                "com.android.settings.com.android.settings.Settings$ChannelNotificationSettingsActivity", "Channel",
                "com.android.settings.com.android.settings.Settings$ConfigureNotificationSettingsActivity", "Config");

        testUniquify(
                "com.android.settings.com.android.settings.Settings$ZenModeEventRuleSettingsActivity", "Zen Event",
                "com.android.settings.com.android.settings.Settings$ZenModeExternalRuleSettingsActivity", "Zen External",
                "com.android.settings.com.android.settings.Settings$ZenModeScheduleRuleSettingsActivity", "Zen Schedule"
        );

        testUniquify(
                "com.android.settings.com.android.settings.Settings$AllApplicationsActivity", "All",
                "com.android.settings.com.android.settings.Settings$ManageApplicationsActivity", "Manage"
        );

        testUniquify(
                "com.google.android.calendar.com.android.calendar.AllInOneActivity", "Google",
                "com.samsung.android.calendar.com.android.calendar.AllInOneActivity", "Samsung"
        );

        testUniquify(
                "com.android.settings.com.android.settings.Settings$AdvancedAppsActivity", "Advanced Apps",
                "com.android.settings.com.android.settings.WebViewImplementation", "Web View"
        );

        testUniquify(
                "com.google.android.googlequicksearchbox.com.google.android.apps.gsa.velvet.ui.settings.PublicSettingsActivity", "Settings",
                "com.google.android.googlequicksearchbox.com.google.android.googlequicksearchbox.SearchActivity", "Search"
        );

        testUniquify(
                "com.android.settings.com.android.settings.Settings$NotificationSettingsActivity", "Notifications",
                "com.android.settings.com.android.settings.Settings$SoundSettingsActivity", "Sound"
        );

        testUniquify(
                "com.android.settings.com.android.settings.Settings$NfcSettingsActivity", "NFC",
                "com.android.settings.com.android.settings.Settings$WriteSettingsActivity", "Write"
        );
    }

    @Test
    public void shouldUniquifyPlatsbanken() {
        testUniquify(
                "com.arbetsformedlingen.activity.com.arbetsformedlingen.activity.SplashActivity", "Splash",
                "se.arbetsformedlingen.platsbanken_labs.se.knowit.platsbankenlabs.MainActivity", "Main");
    }

    @Test
    public void shouldUniquifySvtPlay() {
        testUniquify(
                "air.se.svt.svti.android.svtplayer.air.se.svt.svti.android.svtplayer.AppEntry", "AppEntry",
                "se.svt.android.svtplay.se.svt.svtplay.ui.LauncherActivity", null
        );
    }

    @Test
    public void shouldUniquifyDoNotDisturb() {
        testUniquify(
                "com.android.settings.com.android.settings.Settings$ZenModeAutomationSettingsActivity", "Automation",
                "com.android.settings.com.android.settings.Settings$ZenModeSettingsActivity", null);
    }

    @Test
    public void shouldUniquifyKingOfMathJunior() {
        testUniquify(
                "com.oddrobo.komj.com.oddrobo.komj.activities.LoadActivity", null,
                "com.oddrobo.komjfree.com.oddrobo.komj.activities.LoadActivity", "Free");

        // But not if we only have Free
        testUniquify(
                "com.oddrobo.komjfree.com.oddrobo.komj.activities.LoadActivity", null);
    }

    @Test
    public void testKeepOnlyNamedParts() {
        String string = "IAmAnABCBook";
        Assert.assertThat(Uniquifier.keepOnlyNamedParts(string,
                new HashSet<>(Arrays.asList("I", "ABC", "Book"))), is("IABCBook"));
    }

    @Test
    public void testUniquifyParts() {
        Set<String> parts1 = new HashSet<>(Arrays.asList("some", "non", "unique", "parts"));
        Set<String> parts2 = new HashSet<>(Arrays.asList("x", "some", "non", "unique", "parts"));
        Set<String> parts3 = new HashSet<>(Arrays.asList("y", "z", "some", "non", "unique", "parts"));

        Uniquifier.uniquifyParts(Arrays.asList(parts1, parts2, parts3));

        Assert.assertThat(parts1, is(new HashSet<>()));
        Assert.assertThat(parts2, is(new HashSet<>(Collections.singletonList("x"))));
        Assert.assertThat(parts3, is(new HashSet<>(Arrays.asList("y", "z"))));
    }

    @Test
    public void testSplitInCamelParts() {
        Assert.assertThat(Uniquifier.splitInCamelParts("IAmAnABCBook"),
                is(Arrays.asList("I", "Am", "An", "ABC", "Book")));

        Assert.assertThat("Ends with single word",
                Uniquifier.splitInCamelParts("WhoAmI"),
                is(Arrays.asList("Who", "Am", "I")));
    }
}
