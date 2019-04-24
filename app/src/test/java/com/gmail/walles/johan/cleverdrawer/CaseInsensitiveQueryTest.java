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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class CaseInsensitiveQueryTest {
    @Test
    public void emptyQueryShouldMatchAnything() {
        assertThat(new CaseInsensitiveQuery("").matches(new CaseInsensitive("")), is(true));
        assertThat(new CaseInsensitiveQuery("").matches(new CaseInsensitive("gris")), is(true));
    }

    @Test
    public void testSimpleMatch() {
        assertThat(new CaseInsensitiveQuery("apa").matches(new CaseInsensitive("GAPA")), is(true));
        assertThat(new CaseInsensitiveQuery("APA").matches(new CaseInsensitive("gapa")), is(true));

        assertThat(new CaseInsensitiveQuery("hej").matches(new CaseInsensitive("nej")), is(false));
    }

    @Test
    public void testRealWorldMatch() {
        // Note trailing space in the query string
        assertThat(new CaseInsensitiveQuery("telenor ").matches(new CaseInsensitive("Mitt Telenor")), is(true));
    }

    @Test
    public void testAllWordsAsSubstrings() {
        assertThat(new CaseInsensitiveQuery("mitt telenor").matches(new CaseInsensitive("Mitt Telenor")), is(true));
        assertThat(new CaseInsensitiveQuery("telenor mitt").matches(new CaseInsensitive("Mitt Telenor")), is(true));
        assertThat(new CaseInsensitiveQuery("tele it").matches(new CaseInsensitive("Mitt Telenor")), is(true));
        assertThat(new CaseInsensitiveQuery("elen elen").matches(new CaseInsensitive("Mitt Telenor")), is(true));

        assertThat(new CaseInsensitiveQuery("mitt gris").matches(new CaseInsensitive("Mitt Telenor")), is(false));
        assertThat(new CaseInsensitiveQuery("telenor gris").matches(new CaseInsensitive("Mitt Telenor")), is(false));
    }

    @Test
    public void testExtraSpacing() {
        assertThat(new CaseInsensitiveQuery("mitt  telenor").matches(new CaseInsensitive("Mitt Telenor")), is(true));
        assertThat(new CaseInsensitiveQuery(" mitt telenor").matches(new CaseInsensitive("Mitt Telenor")), is(true));
        assertThat(new CaseInsensitiveQuery("mitt telenor ").matches(new CaseInsensitive("Mitt Telenor")), is(true));
        assertThat(new CaseInsensitiveQuery("  mitt  telenor   ").matches(new CaseInsensitive("Mitt Telenor")), is(true));
    }
}
