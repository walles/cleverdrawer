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

package com.gmail.walles.johan.cleverdrawer.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

public class CaseInsensitive implements Comparable<CaseInsensitive> {
    private final String original;

    @Nullable
    private String lowercase;

    /**
     * Create a possibly null CaseInsensitive from a possibly null CharSequence.
     */
    @Nullable
    public static CaseInsensitive create(@Nullable CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }

        return new CaseInsensitive(charSequence);
    }

    public CaseInsensitive(CharSequence charSequence) {
        original = charSequence.toString();
    }

    private String getLowercase() {
        if (lowercase == null) {
            lowercase = original.toLowerCase(Locale.getDefault());
        }
        return lowercase;
    }

    public boolean contains(CaseInsensitive substring) {
        return getLowercase().contains(substring.getLowercase());
    }

    @Override
    public int hashCode() {
        return original.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof CaseInsensitive)) {
            return false;
        }

        return original.equals(((CaseInsensitive)o).original);
    }

    @Override
    public String toString() {
        return original;
    }

    @Override
    public int compareTo(@NonNull CaseInsensitive o) {
        return original.compareTo(o.original);
    }

    public boolean isEmpty() {
        return original.isEmpty();
    }
}
