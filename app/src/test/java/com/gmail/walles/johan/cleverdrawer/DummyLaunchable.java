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

import android.content.Intent;
import android.graphics.drawable.Drawable;

class DummyLaunchable extends Launchable {
    protected DummyLaunchable(String id) {
        super(id);
        setName(new CaseInsensitive(getId()));
    }

    @Override
    public Drawable getIcon() {
        return null;
    }

    @Override
    public boolean contains(CaseInsensitive substring) {
        return false;
    }

    @Override
    public double getScoreFactor() {
        return 0;
    }

    @Override
    public Intent getLaunchIntent() {
        return null;
    }
}