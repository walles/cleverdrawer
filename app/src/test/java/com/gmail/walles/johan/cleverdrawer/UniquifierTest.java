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

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class UniquifierTest {
    private void testUniquify(String ... args) {
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

        new Uniquifier().uniquify(launchables);
        List<String> actualUniquifiers = new LinkedList<>();
        for (Launchable launchable: launchables) {
            String name = launchable.getName().toString();
            String uniquifier = name.substring(2, name.length() - 1);
            actualUniquifiers.add(uniquifier);
        }

        Assert.assertThat(actualUniquifiers, is(expectedUniquifiers));
    }

    @Test
    public void testUniquify() {
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
}
