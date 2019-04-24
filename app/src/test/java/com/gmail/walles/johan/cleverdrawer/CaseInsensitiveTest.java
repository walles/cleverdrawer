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

import org.junit.Assert;
import org.junit.Test;

public class CaseInsensitiveTest {
    @Test
    public void testContainsInsensitiveness() {
        Assert.assertThat(
                new CaseInsensitive("Johan").contains(new CaseInsensitive("Johan")), is(true));
        Assert.assertThat(
                new CaseInsensitive("Johan").contains(new CaseInsensitive("johan")), is(true));
        Assert.assertThat(
                new CaseInsensitive("johan").contains(new CaseInsensitive("Johan")), is(true));
        Assert.assertThat(
                new CaseInsensitive("johan").contains(new CaseInsensitive("johan")), is(true));

        Assert.assertThat(
                new CaseInsensitive("johan").contains(new CaseInsensitive("hej")), is(false));
    }

    @Test
    public void testContainsSubstring() {
        Assert.assertThat(
                new CaseInsensitive("johan").contains(new CaseInsensitive("jo")), is(true));
        Assert.assertThat(
                new CaseInsensitive("johan").contains(new CaseInsensitive("han")), is(true));
        Assert.assertThat(
                new CaseInsensitive("johan").contains(new CaseInsensitive("oha")), is(true));
    }
}
