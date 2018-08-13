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

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

/**
 * Helps not reshuffling the list as much when things change place.
 */
public class StabilityUtils {
    // Changing this can break the unit tests without actually being wrong, fix the unit tests in
    // that case.
    private static int GROUP_SIZE = 4;

    @CheckResult
    public static List<Launchable> stabilize(File lastSortOrder, List<Launchable> launchables) {
        return stabilize(loadIdOrder(lastSortOrder), launchables);
    }

    @CheckResult
    static List<Launchable> stabilize(List<String> lastOrderIds, List<Launchable> launchables) {
        List<Launchable> stabilized = new ArrayList<>(launchables.size());

        Iterator<Launchable> source = launchables.iterator();
        Iterator<String> lastOrder = lastOrderIds.iterator();

        while (true) {
            // Pick GROUP_SIZE elements from both lists
            ArrayList<Launchable> launchableGroup = getNextGroup(source);
            ArrayList<String> idGroup = getNextGroup(lastOrder);

            if (launchableGroup == null) {
                // No more launchables, done!
                return stabilized;
            }

            if (idGroup == null) {
                // No more stabilization data, add the rest of the launchables as they are and go

                // Add the launchableGroup to stabilized
                stabilized.addAll(launchableGroup);

                // Add the rest of the elements in launchables to stabilized
                while (source.hasNext()) {
                    stabilized.add(source.next());
                }

                return stabilized;
            }

            // Stabilize this group and add it to our result
            stabilized.addAll(Arrays.asList(stabilizeGroup(launchableGroup, idGroup)));
        }
    }

    private static Launchable[] stabilizeGroup(
            ArrayList<Launchable> launchableGroup,
            ArrayList<String> idGroup)
    {
        Launchable[] stabilized = new Launchable[launchableGroup.size()];

        // Add all launchables that already have spots in the right places
        int idIndex = 0;
        for (String id: idGroup) {
            Launchable launchable = removeById(launchableGroup, id);
            stabilized[idIndex++] = launchable;
        }

        // Fill in the blanks from the remaining launchables
        for (int i = 0; i < stabilized.length; i++) {
            if (stabilized[i] == null) {
                stabilized[i] = launchableGroup.remove(0);
            }
        }

        return stabilized;
    }

    /**
     * If there is a Launchable with the given ID, remove it from the list and return it.
     *
     * @return A Launchable with the given ID, or null if not found
     */
    @Nullable
    private static Launchable removeById(ArrayList<Launchable> launchables, String id) {
        Iterator<Launchable> launchableIter = launchables.iterator();
        while (launchableIter.hasNext()) {
            Launchable launchable = launchableIter.next();
            if (id.equals(launchable.getId())) {
                launchableIter.remove();
                return launchable;
            }
        }

        // ID not found
        return null;
    }

    /**
     * Get a group of {@link #GROUP_SIZE} elements from an iterator.
     *
     * @return null if the iterator has no more elements, or fewer elements if we can't get enough
     */
    @Nullable
    private static <E> ArrayList<E> getNextGroup(Iterator<E> source) {
        if (!source.hasNext()) {
            return null;
        }

        ArrayList<E> returnMe = new ArrayList<>(GROUP_SIZE);
        while (true) {
            if (returnMe.size() >= GROUP_SIZE) {
                return returnMe;
            }

            if (!source.hasNext()) {
                return returnMe;
            }

            returnMe.add(source.next());
        }
    }

    public static void storeOrder(File lastSortOrder, List<Launchable> launchables) {
        // Write to a temp file first...
        File tempfile = new File(lastSortOrder.toString() + ".tmp");
        try (PrintWriter out = new PrintWriter(tempfile)) {
            for (Launchable launchable: launchables) {
                FIXME: Store only the IDs that the user has clicked

                out.println(launchable.getId());
            }
        } catch (IOException e) {
            Timber.w(e, "Unable to store sort order into: %s", tempfile);
            if (!tempfile.delete()) {
                Timber.w("Unable to delete temporary sort order file: %s", tempfile);
            }
            if (lastSortOrder.exists() && !lastSortOrder.delete()) {
                Timber.w("Unable to delete sort order file: %s", lastSortOrder);
            }
            return;
        }

        // Atomically overwrite the actual sort order file
        if (!tempfile.renameTo(lastSortOrder)) {
            Timber.w("Unable to replace sort order file: %s", lastSortOrder);
            if (!lastSortOrder.delete()) {
                Timber.w("Unable to delete sort order file: %s", lastSortOrder);
            }
            if (tempfile.exists() && !tempfile.delete()) {
                Timber.w("Unable to delete temporary sort order file: %s", tempfile);
            }
        }
    }

    static List<String> loadIdOrder(File lastOrder) {
        if (!lastOrder.exists()) {
            // This is OK; it will happen before we've started tracking the last sort order for the
            // first time.
            Timber.i("Last order file doesn't exist: %s", lastOrder);
            return Collections.emptyList();
        }

        List<String> idList = new LinkedList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader(lastOrder))) {
            for(String line; (line = reader.readLine()) != null; ) {
                idList.add(line);
            }
        } catch (IOException e) {
            Timber.w(e, "Unable to read sort order from: %s", lastOrder);
            return Collections.emptyList();
        }

        return idList;
    }
}
