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

import org.jetbrains.annotations.TestOnly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

/**
 * Helps not reshuffling the list as much when things change place.
 */
public class StabilityUtils {
    public static void stabilize(File lastSortOrder, List<Launchable> launchables) {

    }

    @TestOnly
    static List<Launchable> stabilize(List<String> lastOrderIds, List<Launchable> launchables) {
        return Collections.emptyList();
    }

    public static void storeOrder(File lastSortOrder, List<Launchable> launchables) {
        // Write to a temp file first...
        File tempfile = new File(lastSortOrder.toString() + ".tmp");
        try (PrintWriter out = new PrintWriter(tempfile)) {
            for (Launchable launchable: launchables) {
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

    @TestOnly
    static List<String> loadIdOrder(File lastOrder) {
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
