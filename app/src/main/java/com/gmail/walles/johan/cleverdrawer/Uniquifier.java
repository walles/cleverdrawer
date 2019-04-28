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

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final Pattern CLASS_NAME = Pattern.compile(".*\\.([^$]+)");
    private static final Pattern PACKAGE_NAME = Pattern.compile("^(.*)[.][^.]*$");
    private static final Pattern DOT = Pattern.compile("[.]");

    interface Splitter {
        List<String> split(String string);
    }

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

            if (uniquifyByClassName(list, INNER_CLASS_NAME, Uniquifier::splitInCamelParts)) {
                return;
            }

            if (uniquifyByClassName(list, CLASS_NAME, Uniquifier::splitInCamelParts)) {
                return;
            }

            if (uniquifyByClassName(list, PACKAGE_NAME, Uniquifier::splitByDots)) {
                return;
            }

            // This failure will be logged by the duplicate-names logger, just leave it
        }
    }

    private boolean uniquifyByClassName(List<Launchable> launchables, Pattern classNamePattern, Splitter splitter) {
        List<String> classNames = new LinkedList<>();
        List<Set<String>> parts = new LinkedList<>();
        for (Launchable launchable: launchables) {
            Matcher matcher = classNamePattern.matcher(launchable.getId());
            if (!matcher.matches()) {
                // No class name, can't compare these, never mind
                return false;
            }

            String className = matcher.group(1);
            classNames.add(className);
            parts.add(new HashSet<>(splitter.split(className)));
        }

        // We now have a set of parts for each class name
        uniquifyParts(parts);

        // We now have a set of unique parts per class name, turn them into decorators
        Iterator<String> classNamesIterator = classNames.iterator();
        Iterator<Set<String>> partsIterator = parts.iterator();
        List<String> decorators = new ArrayList<>(launchables.size());
        while (classNamesIterator.hasNext() && partsIterator.hasNext()) {
            String className = classNamesIterator.next();
            Set<String> partNames = partsIterator.next();
            String decoration = keepOnlyNamedParts(className, partNames, splitter);
            decorators.add(decoration);
        }

        if (hasDuplicates(decorators)) {
            return false;
        }

        Iterator<Launchable> launchableIterator = launchables.iterator();
        Iterator<String> decorationsIterator = decorators.iterator();
        while (launchableIterator.hasNext() && decorationsIterator.hasNext()) {
            Launchable launchable = launchableIterator.next();
            String decoration = decorationsIterator.next();

            if (decoration.isEmpty()) {
                continue;
            }

            String decorated = launchable.getName().toString() + "(" + titleCase(decoration) + ")";
            launchable.setName(new CaseInsensitive(decorated));
        }

        return true;
    }

    private boolean hasDuplicates(List<String> strings) {
        if (strings.size() <= 1) {
            return false;
        }

        Set<String> alreadyFound = new HashSet<>();
        for (String string: strings) {
            if (alreadyFound.contains(string)) {
                return true;
            }
            alreadyFound.add(string);
        }

        return false;
    }

    @VisibleForTesting static String keepOnlyNamedParts(String string, Set<String> keepThese, Splitter splitter) {
        StringBuilder builder = new StringBuilder();
        for (String part: splitter.split(string)) {
            if (keepThese.contains(part)) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }

                builder.append(part);
            }
        }

        return builder.toString();
    }

    @VisibleForTesting static void uniquifyParts(List<Set<String>> parts) {
        // Count how many sets each string is part of
        Map<String, Integer> partCounts = new HashMap<>();
        for (Set<String> set: parts) {
            for (String string: set) {
                Integer count = partCounts.get(string);
                if (count == null) {
                    count = 0;
                }

                partCounts.put(string, count + 1);
            }
        }

        // List which we have more than one of
        Set<String> duplicateParts = new HashSet<>();
        for (Map.Entry<String, Integer> entry: partCounts.entrySet()) {
            String string = entry.getKey();
            int count = entry.getValue();

            if (count > 1) {
                duplicateParts.add(string);
            }
        }

        for (Set<String> part: parts) {
            part.removeAll(duplicateParts);
        }
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

    private static List<String> splitByDots(String string) {
        return Arrays.asList(DOT.split(string));
    }

    // From: https://stackoverflow.com/a/1086134/473672
    private static String titleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }
}
