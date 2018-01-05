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

import java.util.LinkedList;
import java.util.List;

/**
 * Time some events and present a human readable rendering of how long things took.
 */
public class Timer {
    private final long t0;
    private static class Leg {
        public final String name;
        public final long start;
        public Leg(String name, long start) {
            this.name = name;
            this.start = start;
        }
    }
    private List<Leg> legs;

    public Timer() {
        t0 = System.currentTimeMillis();
    }

    public void addLeg(String name) {
        if (legs == null) {
            legs = new LinkedList<>();
        }
        legs.add(new Leg(name, System.currentTimeMillis()));
    }

    /**
     * @return "47ms" or "100ms = 13ms setup + 87ms something else"
     */
    @Override
    public String toString() {
        long now = System.currentTimeMillis();
        if (legs == null) {
            long dtMs = now - t0;
            //noinspection StringConcatenationMissingWhitespace
            return dtMs + "ms";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(now - t0);
        builder.append("ms = ");

        long lastT0 = t0;
        String name = "setup";
        boolean firstLap = true;
        for (Leg leg : legs) {
            if (!firstLap) {
                builder.append(" + ");
            }

            long dt = leg.start - lastT0;
            builder.append(dt);
            builder.append("ms ");
            builder.append(name);

            firstLap = false;
            name = leg.name;
            lastT0 = leg.start;
        }

        builder.append(" + ");
        long lastLegStart = legs.get(legs.size() - 1).start;
        builder.append(now - lastLegStart);
        builder.append("ms ");
        builder.append(name);

        return builder.toString();
    }
}
