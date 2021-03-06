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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import org.jetbrains.annotations.TestOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class IntentLaunchable extends Launchable {
    private ResolveInfo resolveInfo;
    private PackageManager packageManager;

    private Drawable icon;

    protected final Intent launchIntent;

    protected IntentLaunchable(ResolveInfo resolveInfo, PackageManager packageManager) {
        super(resolveInfo.activityInfo.applicationInfo.packageName + "." + resolveInfo.activityInfo.name);

        this.resolveInfo = resolveInfo;
        this.packageManager = packageManager;

        // Fast!
        this.launchIntent = createLaunchIntent(resolveInfo);
    }

    @TestOnly
    public IntentLaunchable(String id, @NonNull CaseInsensitive name) {
        super(id);
        setName(name);
        this.launchIntent = null;
    }

    private static List<Launchable> loadSettingsLaunchables(Context context) {
        Timer timer = new Timer();
        final PackageManager packageManager = context.getPackageManager();

        timer.addLeg("Listing Settings Query Intents");
        List<ResolveInfo> resInfos = new LinkedList<>();
        for (Intent intent: getSettingsQueryIntents()) {
            resInfos.addAll(packageManager.queryIntentActivities(intent, 0));
        }

        timer.addLeg("Creating Settings Launchables");
        List<Launchable> launchables = new ArrayList<>();
        for(ResolveInfo resolveInfo : resInfos) {
            launchables.add(new IntentLaunchable(resolveInfo, packageManager));
        }

        Timber.i("loadIntentLaunchables() timings: %s", timer);
        return launchables;
    }

    /**
     * @return A collection of launchables. No duplicate IDs, but zero or more Launchables may have
     * empty names.
     */
    public static List<Launchable> loadLaunchables(Context context) {
        List<Launchable> launchables = new ArrayList<>(AppLaunchable.loadAppLaunchables(context));
        launchables.addAll(loadSettingsLaunchables(context));
        return launchables;
    }

    private static Iterable<Intent> getSettingsQueryIntents() {
        List<Intent> queryIntents = new LinkedList<>();

        for (String settingsAction: getSettingsActions()) {
            queryIntents.add(new Intent(settingsAction));
        }

        // Battery screen on my Galaxy S6, I want to find this when searching for "battery"
        queryIntents.add(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));

        return queryIntents;
    }

    private static Iterable<String> getSettingsActions() {
        List<String> actions = new LinkedList<>();
        for (Field field: Settings.class.getFields()) {
            if (!Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            if (!Objects.equals(field.getType(), String.class)) {
                continue;
            }
            if (!field.getName().startsWith("ACTION_")) {
                continue;
            }

            String value;
            try {
                value = (String)field.get(null);
            } catch (IllegalAccessException e) {
                Timber.w(e, "Unable to get field value of %s.%s",
                        Settings.class.getName(), field.getName());
                continue;
            }

            if (value == null) {
                Timber.w("Got null value for field %s.%s",
                        Settings.class.getName(), field.getName());
                continue;
            }

            if (!value.endsWith("_SETTINGS")) {
                continue;
            }

            actions.add(value);
        }

        return actions;
    }

    @Override
    public Drawable getIcon() {
        if (icon == null) {
            // Slow!
            icon = resolveInfo.loadIcon(packageManager);
        }

        return icon;
    }

    @Override
    @Nullable
    protected CaseInsensitive doGetTrueName() {
        if (resolveInfo != null) {
            // Slow!
            return new CaseInsensitive(resolveInfo.loadLabel(packageManager).toString());
        }

        return null;
    }

    private static Intent createLaunchIntent(
            ResolveInfo resolveInfo)
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        ActivityInfo activityInfo = resolveInfo.activityInfo;
        ComponentName componentName =
                new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);

        intent.setComponent(componentName);

        return intent;
    }

    @Override
    public boolean matches(CaseInsensitiveQuery query) {
        return query.matches(getName());
    }

    @Override
    public double getScoreFactor() {
        if (getId().startsWith("com.android.settings.") || getId().startsWith("android.settings.")) {
            // Put settings after apps. Multiple reasons really:
            // * People expect apps, not settings, so put what people expect first
            // * All the settings have the same icon, mixing this with the apps makes both apps and
            //  settings harder to find
            return 0.99;
        }

        return 1.0;
    }

    @Override
    public Intent getLaunchIntent() {
        return launchIntent;
    }

    @Nullable
    @Override
    public Intent getAppInfoIntent() {
        return null;
    }
}
