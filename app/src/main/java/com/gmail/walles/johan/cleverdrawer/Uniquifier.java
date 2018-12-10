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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Uniquifier {
    /**
     * Qualifiers to add to various IDs to give the unique names.
     * <p>
     * Without these, a number of things end up with the same name, and are impossible for the user
     * to tell apart.
     *
     * @see LaunchableAdapter#logDuplicateNames(List)
     */
    private static final Map<String, String> UNIQUIFIERS = new HashMap<>(); static {
        // Emulator with Android 8.0 Oreo
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$AppNotificationSettingsActivity", "App");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$ChannelNotificationSettingsActivity", "Channel");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$ConfigureNotificationSettingsActivity", "Config");

        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$ZenModeEventRuleSettingsActivity", "Zen Event");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$ZenModeExternalRuleSettingsActivity", "Zen External");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$ZenModeScheduleRuleSettingsActivity", "Zen Schedule");

        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$AllApplicationsActivity", "All");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$ManageApplicationsActivity", "Manage");

        // Samsung Galaxy S6 with Android 7.0
        UNIQUIFIERS.put("com.google.android.calendar.com.android.calendar.AllInOneActivity", "Google");
        UNIQUIFIERS.put("com.samsung.android.calendar.com.android.calendar.AllInOneActivity", "Samsung");

        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$AdvancedAppsActivity", "Advanced Apps");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.WebViewImplementation", "Web View");

        UNIQUIFIERS.put("com.google.android.googlequicksearchbox.com.google.android.apps.gsa.velvet.ui.settings.PublicSettingsActivity", "Settings");
        UNIQUIFIERS.put("com.google.android.googlequicksearchbox.com.google.android.googlequicksearchbox.SearchActivity", "Search");

        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$NotificationSettingsActivity", "Notifications");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$SoundSettingsActivity", "Sound");

        // Samsung Galaxy S5 Neo, 6.0.1
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$NfcSettingsActivity", "NFC");
        UNIQUIFIERS.put("com.android.settings.com.android.settings.Settings$WriteSettingsActivity", "Write");
    }

    public void uniquify(List<Launchable> launchables) {
        for (Launchable launchable: launchables) {
            String uniquifier = UNIQUIFIERS.get(launchable.getId());
            if (uniquifier == null) {
                continue;
            }

            launchable.setName(new CaseInsensitive(launchable.getName() + " (" + uniquifier + ")"));
        }
    }

}
