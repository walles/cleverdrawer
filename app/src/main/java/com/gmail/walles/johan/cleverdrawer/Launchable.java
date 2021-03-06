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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import timber.log.Timber;

@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
public abstract class Launchable implements Comparable<Launchable> {
    private final String id;
    private CaseInsensitive name;

    @Nullable
    private Double score;

    /**
     * Calling this method can be slow!
     */
    public abstract Drawable getIcon();

    protected Launchable(String id) {
        this.id = id;
    }

    public boolean hasName() {
        return name != null;
    }

    @NonNull
    public CaseInsensitive getName() {
        if (name != null) {
            return name;
        }

        CaseInsensitive trueName = getTrueName();
        if (trueName != null) {
            name = trueName;
            return name;
        }

        Timber.w(new IllegalStateException("getName() called before setName() for: " + id));
        return new CaseInsensitive("");
    }

    public void setName(@NonNull CaseInsensitive name) {
        //noinspection ConstantConditions
        if (name == null) {
            // Seems like this could happen some times according to Crashlytics, log diagnostics!
            Timber.w(new NullPointerException("setName called with Null value"));
        }
        this.name = name;
    }

    /**
     * Get true name from system, calling this can be slow!
     */
    @Nullable
    public final CaseInsensitive getTrueName() {
        CaseInsensitive trueName = doGetTrueName();
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
    protected CaseInsensitive doGetTrueName() {
        return null;
    }

    public abstract boolean matches(CaseInsensitiveQuery query);

    public void setScore(double score) {
        if (score <= 0.0) {
            // Score must be > 0 so that we can multiply it by a factor below
            throw new IllegalArgumentException("score must be > 0, was " + score);
        }

        this.score = score * getScoreFactor();
    }

    protected abstract double getScoreFactor();

    public String getId() {
        return id;
    }

    public abstract Intent getLaunchIntent();

    private double getScore() {
        if (score != null) {
            return score;
        }
        return 1.0 * getScoreFactor();
    }

    @Override
    public int compareTo(@NonNull Launchable o) {
        int scoresCompare = Double.compare(o.getScore(), getScore());
        if (scoresCompare != 0) {
            return scoresCompare;
        }

        return getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return getId();
    }

    public boolean hasScore() {
        return score != null;
    }

    /**
     * The intent returned here will be launched when the user long-presses an icon
     * and chooses "App Info" from the popup menu.
     *
     * @return null if managing this kind of launcher is unsupported
     */
    @Nullable
    public abstract Intent getAppInfoIntent();
}
