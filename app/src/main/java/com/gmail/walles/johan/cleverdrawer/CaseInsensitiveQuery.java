/*
 * MIT License
 *
 * Copyright (c) 2019 Johan Walles <johan.walles@gmail.com>
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

import java.util.ArrayList;
import java.util.List;

public class CaseInsensitiveQuery {
    private final List<CaseInsensitive> words = new ArrayList<>(1);

    public CaseInsensitiveQuery(String queryString) {
        // From: https://stackoverflow.com/a/7899558/473672
        for (String word: queryString.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            words.add(new CaseInsensitive(word));
        }
    }

    public boolean matches(CaseInsensitive tryMe) {
        for (CaseInsensitive word: words) {
            if (!tryMe.contains(word)) {
                return false;
            }
        }

        return true;
    }
}