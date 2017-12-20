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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class Launchable implements Comparable<Launchable> {
    private final String id;
    private String name;
    private double score;

    public abstract Drawable getIcon();

    protected Launchable(String id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        if (name != null) {
            return name;
        }

        name = getTrueName();
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Get true name from system, calling this can be slow!
     */
    @Nullable
    public final String getTrueName() {
        String trueName = doGetTrueName();
        if (trueName != null) {
            return trueName;
        }

        return name;
    }

    /**
     * Get true name from system, calling this can be slow!
     * <p>
     * Depending on what type of launchable you are you may or may not need to override this method.
     *
     * @return null if true name unknown
     */
    @Nullable
    protected String doGetTrueName() {
        return null;
    }


    /**
     * @param search This should be a lowercase search string.
     */
    public abstract boolean matches(CharSequence search);

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

    public String getId() {
        return id;
    }

    public abstract Intent getLaunchIntent();

    @Override
    public int compareTo(@NonNull Launchable o) {
        int scoresCompare = Double.compare(o.score, score);
        if (scoresCompare != 0) {
            return scoresCompare;
        }

        return getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return getId();
    }
}
