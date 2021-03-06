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

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

class Uniquifier {
    private static final Pattern INNER_ONLY = Pattern.compile("^.*\\$(.*)$");
    private static final Pattern CLASS_NAME = Pattern.compile("^.*?([^.]*)$");
    private static final Pattern ORG_NAME = Pattern.compile("^((com)|(org))[.]([^.]+)[.].*$");
    private static final Pattern ALL = Pattern.compile("^(.*)$");
    private static final Pattern DOT = Pattern.compile("[.]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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

        for (List<Launchable> sameNamedLaunchables: nameToLaunchables.values()) {
            if (sameNamedLaunchables.size() == 1) {
                continue;
            }

            if (uniquifyByType(sameNamedLaunchables)) {
                continue;
            }

            if (uniquifyByOrgName(sameNamedLaunchables)) {
                continue;
            }

            if (uniquifyByIdParts(sameNamedLaunchables, INNER_ONLY)) {
                continue;
            }

            if (uniquifyByIdParts(sameNamedLaunchables, CLASS_NAME)) {
                continue;
            }

            uniquifyByIdParts(sameNamedLaunchables, ALL);
        }
    }

    private boolean uniquifyByType(List<Launchable> sameNamedLaunchables) {
        List<String> typeDecorators = new ArrayList<>(sameNamedLaunchables.size());
        for (Launchable launchable: sameNamedLaunchables) {
            String decorator;
            if (launchable instanceof ContactLaunchable) {
                decorator = "Contact";
            } else if (launchable instanceof IntentLaunchable) {
                boolean isSettings = launchable.getId().toLowerCase(Locale.getDefault()).contains("settings");
                decorator = isSettings ? "Settings" : "App";
            } else {
                decorator = null;
            }
            typeDecorators.add(decorator);
        }

        return applyDecorators(sameNamedLaunchables, typeDecorators);
    }

    private boolean uniquifyByOrgName(List<Launchable> sameNamedLaunchables) {
        List<String> orgNames = new ArrayList<>(sameNamedLaunchables.size());
        for (Launchable launchable: sameNamedLaunchables) {
            Matcher matcher = ORG_NAME.matcher(launchable.getId());
            if (!matcher.matches()) {
                return false;
            }

            orgNames.add(titleCase(matcher.group(4)));
        }

        return applyDecorators(sameNamedLaunchables, orgNames);
    }

    /**
     * Take a number of launchables sharing the same name and make them not.
     *
     * @return True if we were able to uniquify them, false otherwise.
     */
    private boolean uniquifyByIdParts(List<Launchable> sameNamedLaunchables, Pattern namePartExtractor) {
        final Collection<String> nameParts =
                titleCaseAll(splitBySpace(sameNamedLaunchables.get(0).getName().toString()));

        List<String> classNames = new LinkedList<>();
        List<Set<String>> commonNameParts = new LinkedList<>();
        for (Launchable launchable: sameNamedLaunchables) {
            Matcher matcher = namePartExtractor.matcher(launchable.getId());
            if (!matcher.matches()) {
                return false;
            }
            String completeClassName = launchable.getId();
            String className = matcher.group(1);

            classNames.add(className);

            // Use the fully qualified class name here, this is by design. Change it and see which
            // unit test fails!
            Set<String> classNameParts = new HashSet<>(tokenize(completeClassName));
            classNameParts.addAll(nameParts);
            commonNameParts.add(new HashSet<>(classNameParts));
        }

        // We now have a set of parts for each class name
        uniquifyParts(commonNameParts);

        // We now have a set of unique parts per class name, turn them into decorators
        Iterator<String> classNamesIterator = classNames.iterator();
        Iterator<Set<String>> partsIterator = commonNameParts.iterator();
        List<String> decorators = new ArrayList<>(sameNamedLaunchables.size());
        while (classNamesIterator.hasNext() && partsIterator.hasNext()) {
            String className = classNamesIterator.next();
            Set<String> partNames = partsIterator.next();
            List<String> decorationParts = keepOnlyNamedParts(className, partNames);
            decorationParts = dedupAndKeepLast(decorationParts);
            decorators.add(joinBySpace(decorationParts));
        }

        return applyDecorators(sameNamedLaunchables, decorators);
    }

    private static boolean applyDecorators(List<Launchable> sameNamedLaunchables,
            List<String> decorators) {
        if (hasDuplicates(decorators)) {
            // Deduplication failed
            return false;
        }

        Iterator<Launchable> launchableIterator = sameNamedLaunchables.iterator();
        Iterator<String> decorationsIterator = decorators.iterator();
        while (launchableIterator.hasNext() && decorationsIterator.hasNext()) {
            Launchable launchable = launchableIterator.next();
            String decoration = decorationsIterator.next();

            if (decoration == null) {
                continue;
            }

            if (decoration.isEmpty()) {
                continue;
            }

            String decorated = launchable.getName().toString() + " (" + titleCase(decoration) + ")";
            launchable.setName(new CaseInsensitive(decorated));

            Timber.i("Uniquified <%s> based on ID <%s>", decorated, launchable.getId());
        }

        return true;
    }

    private static boolean hasDuplicates(List<String> strings) {
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

    @VisibleForTesting static List<String> keepOnlyNamedParts(String string, Set<String> keepThese) {
        List<String> returnMe = new ArrayList<>();
        for (String part: tokenize(string)) {
            if (keepThese.contains(part)) {
                returnMe.add(part);
            }
        }

        return returnMe;
    }

    /**
     * If the list contains duplicates, keep only the last instance of each duplicate.
     *
     * @return A new list with dups removed
     */
    private static List<String> dedupAndKeepLast(List<String> parts) {
        LinkedList<String> returnMe = new LinkedList<>();

        List<String> alreadyUsed = new ArrayList<>();
        for (int i = parts.size() - 1; i >= 0; i--) {
            String part = parts.get(i);

            if (alreadyUsed.contains(part)) {
                continue;
            }
            alreadyUsed.add(part);

            returnMe.addFirst(part);
        }

        return returnMe;
    }

    private static String joinBySpace(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part: parts) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
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

    /**
     * Given a fully qualified class name "adam.bertil.caesar.David$Erik", tokenize it into "Adam",
     * "Bertil", "Caesar", "David", "Erik".
     */
    @VisibleForTesting
    static List<String> tokenize(String string) {
        int lastDotIndex = string.lastIndexOf('.');
        List<String> tokens = new ArrayList<>();
        if (lastDotIndex >= 0) {
            tokens.addAll(titleCaseAll(splitByDots(string.substring(0, lastDotIndex))));
        }

        int dollarIndex = string.indexOf('$');
        if (dollarIndex >= 0 && lastDotIndex > dollarIndex) {
            // We have . after $, this makes no sense in a class name
            throw new IllegalArgumentException("Dot after dollar, not a valid class name: <" + string + ">");
        }

        if (dollarIndex == -1) {
            // No $ in the string
            tokens.addAll(splitInCamelParts(string.substring(lastDotIndex + 1)));
        } else {
            tokens.addAll(splitInCamelParts(string.substring(lastDotIndex + 1, dollarIndex)));
            tokens.addAll(splitInCamelParts(string.substring(dollarIndex + 1)));
        }

        return tokens;
    }

    private static List<String> splitByDots(String string) {
        return Arrays.asList(DOT.split(string));
    }

    private static List<String> splitBySpace(String string) {
        return Arrays.asList(WHITESPACE.split(string));
    }

    private static List<String> titleCaseAll(List<String> toTitleCase) {
        List<String> returnMe = new ArrayList<>(toTitleCase.size());
        for (String input: toTitleCase) {
            returnMe.add(titleCase(input));
        }

        return returnMe;
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
            } else {
                c = Character.toLowerCase(c);
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }
}
