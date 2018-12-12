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

import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Uniquifier {
    private static final Pattern INNER_CLASS_NAME = Pattern.compile(".*\\$(.*)");

    public void uniquify(List<Launchable> launchables) {
        Map<String, List<Launchable>> nameToLaunchables = new HashMap<>();

        for (Launchable launchable: launchables) {
            String name = launchable.getName().toString();
            List<Launchable> list = nameToLaunchables.get(name);
            if (list == null) {
                list = new LinkedList<>();
                nameToLaunchables.put(name, list);
            }

            list.add(launchable);
        }

        for (List<Launchable> list: nameToLaunchables.values()) {
            if (list.size() == 1) {
                continue;
            }

            if (uniquifyByInnerClassName(list)) {
                return;
            }

            // This failure will be logged by the duplicate-names logger, just leave it
        }
    }

    private boolean uniquifyByInnerClassName(List<Launchable> launchables) {
        List<String> innerClassNames = new LinkedList<>();
        List<Set<String>> parts = new LinkedList<>();
        for (Launchable launchable: launchables) {
            Matcher matcher = INNER_CLASS_NAME.matcher(launchable.getId());
            if (!matcher.matches()) {
                // No inner class name, can't compare these, never mind
                return false;
            }

            String innerClassName = matcher.group(1);
            innerClassNames.add(innerClassName);
            parts.add(new HashSet<>(splitInCamelParts(innerClassName)));
        }

        // We now have a set of parts for each inner class name

        uniquifyParts(parts);

        // We now have a set of unique parts per inner class name

        Iterator<Launchable> launchableIterator = launchables.iterator();
        Iterator<String> innerClassNamesIterator = innerClassNames.iterator();
        Iterator<Set<String>> partsIterator = parts.iterator();
        while (launchableIterator.hasNext() && innerClassNamesIterator.hasNext() && partsIterator.hasNext()) {
            Launchable launchable = launchableIterator.next();
            String innerClassName = innerClassNamesIterator.next();
            Set<String> partNames = partsIterator.next();
            String decoration = keepOnlyNamedParts(innerClassName, partNames);

            String decorated = launchable.getName().toString() + "(" + decoration + ")";
            launchable.setName(new CaseInsensitive(decorated));
        }

        return true;
    }

    @VisibleForTesting static String keepOnlyNamedParts(String string, Set<String> partNames) {
        return null;
    }

    @VisibleForTesting static void uniquifyParts(List<Set<String>> parts) {
    }

    @VisibleForTesting static List<String> splitInCamelParts(String string) {
        // First word starts here
        List<Integer> wordStarts = new LinkedList<>();

        // Initial letter is a word start
        boolean wasWordStart = true;

        for (int i = 1; i < string.length(); i++) {
            boolean isWordStart = Character.isUpperCase(string.charAt(i));
            if (wasWordStart && !isWordStart) {
                wordStarts.add(i - 1);
            } else if ((!wasWordStart) && isWordStart) {
                wordStarts.add(i);
            }

            wasWordStart = isWordStart;
        }

        List<String> parts = new LinkedList<>();
        int lastWordStart = 0;
        for (int wordStart: wordStarts) {
            if (wordStart == lastWordStart) {
                // Filter out empty parts
                continue;
            }

            parts.add(string.substring(lastWordStart, wordStart));

            lastWordStart = wordStart;
        }
        parts.add(string.substring(lastWordStart));

        return parts;
    }
}
