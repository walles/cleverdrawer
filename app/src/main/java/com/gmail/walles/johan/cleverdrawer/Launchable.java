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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.TestOnly;

import java.util.Locale;

public class Launchable implements Comparable<Launchable> {
    private ResolveInfo resolveInfo;
    private PackageManager packageManager;

    private Drawable icon;

    private String name;

    private Double score;

    /**
     * This is what we have lowercased. Managed by {@link #getLowercaseName()}.
     */
    @Nullable
    private String lowercaseBase;

    /**
     * This is the lower case name. Access through {@link #getLowercaseName()}.
     */
    private String lowercased;

    public final String id;
    public final Intent launchIntent;

    public Launchable(ResolveInfo resolveInfo, PackageManager packageManager) {
        this.resolveInfo = resolveInfo;
        this.packageManager = packageManager;

        // Fast!
        this.launchIntent = createLaunchIntent(resolveInfo);

        // Fast!
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        this.id = activityInfo.applicationInfo.packageName + "." + activityInfo.name;
    }

    public Drawable getIcon() {
        if (icon == null) {
            // Slow!
            icon = resolveInfo.loadIcon(packageManager);
        }

        return icon;
    }

    @Nullable
    public String getName() {
        if (name != null) {
            return name;
        }

        name = getTrueName();
        return name;
    }

    /**
     * Get true name from system, calling this can be slow!
     */
    @Nullable
    public String getTrueName() {
        if (resolveInfo != null) {
            // Slow!
            return resolveInfo.loadLabel(packageManager).toString();
        }

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @TestOnly
    public Launchable(String id, @Nullable String name) {
        this.id = id;
        this.name = name;
        this.icon = null;
        this.launchIntent = null;
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
    public String toString() {
        return id;
    }

    /**
     * @param search This should be a lowercase search string.
     */
    public boolean matches(CharSequence search) {
        String name = getLowercaseName();
        if (name == null) {
            throw new UnsupportedOperationException("My name is null, can't match that, id: " + id);
        }

        return name.toLowerCase(Locale.getDefault()).contains(search);
    }

    @Nullable
    private String getLowercaseName() {
        String name = getName();
        if (name == null) {
            return null;
        }

        if (!name.equals(lowercaseBase)) {
            lowercased = name.toLowerCase(Locale.getDefault());
            lowercaseBase = name;
        }

        return lowercased;
    }

    public void setScore(double score) {
        if (score <= 0.0) {
            // Score must be > 0 so that we can multiply it by a factor below
            throw new IllegalArgumentException("score must be > 0, was " + score);
        }

        double factor = 1.0;
        if (id.startsWith("com.android.settings.") || id.startsWith("android.settings.")) {
            // Put settings after apps. Multiple reasons really:
            // * People expect apps, not settings, so put what people expect first
            // * All the settings have the same icon, mixing this with the apps makes both apps and
            //  settings harder to find
            factor = 0.99;
        }

        this.score = score * factor;
    }

    @Override
    public int compareTo(@NonNull Launchable o) {
        int scoresCompare = Double.compare(o.score, score);
        if (scoresCompare != 0) {
            return scoresCompare;
        }

        return getName().compareTo(o.getName());
    }
}
