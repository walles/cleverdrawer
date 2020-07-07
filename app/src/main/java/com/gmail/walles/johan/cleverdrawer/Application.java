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

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import androidx.annotation.NonNull;
import timber.log.Timber;

// Can't use Timber before logging set up
@SuppressLint("LogNotTimber")
public class Application extends android.app.Application {
    public static final boolean IS_CRASHLYTICS_ENABLED = isCrashlyticsEnabled();

    private static final String TAG = "CleverDrawer";

    @Override
    public void onCreate() {
        Timber.Tree tree;
        if (IS_CRASHLYTICS_ENABLED) {
            tree = new CrashlyticsTree();
        } else {
            tree = new Timber.DebugTree();
        }
        Timber.plant(tree);

        super.onCreate();
    }

    private static boolean isCrashlyticsEnabled() {
        if (!isRunningOnAndroid()) {
            Log.d(TAG, "Not on Android, not logging to Crashlytics");
            return false;
        }
        if (isRunningOnEmulator()) {
            Log.d(TAG, "Emulated Android, not logging to Crashlytics");
            return false;
        }
        // FIXME: if (executing instrumentation tests) { return false; }

        Log.d(TAG, "On a physical Android device, Crashlytics logging enabled");
        return true;
    }

    private static boolean isRunningOnEmulator() {
        // Inspired by
        // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in>
        if (Build.PRODUCT == null) {
            Log.i(TAG, "No PRODUCT, not on emulator");
            return false;
        }

        Set<String> parts = new HashSet<>(Arrays.asList(Build.PRODUCT.split("_")));
        parts.remove("sdk");
        parts.remove("google");
        parts.remove("x86");
        parts.remove("phone");
        parts.remove("gphone");

        // If the build identifier contains only the above keywords in some order, then we're
        // in an emulator
        if (parts.isEmpty()) {
            return true;
        } else {
            Log.i(TAG, "Have non-filtered device parts, not on emulator: " + parts);
            return false;
        }
    }

    private static boolean isRunningOnAndroid() {
        // Inspired by: https://developer.android.com/reference/java/lang/System.html#getProperties()
        // Developed using trial and error...
        final Properties properties = System.getProperties();
        final String httpAgent = properties.getProperty("http.agent");
        if (httpAgent == null) {
            return false;
        }
        return httpAgent.contains("Android");
    }

    private static class CrashlyticsTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, @NonNull String message, Throwable t) {
            final FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.log(message);
            if (t != null) {
                crashlytics.recordException(t);
            }
        }
    }
}
