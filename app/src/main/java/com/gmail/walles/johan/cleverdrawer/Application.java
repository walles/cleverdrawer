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
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

// Can't use Timber before logging set up
@SuppressLint("LogNotTimber")
public class Application extends android.app.Application {
    private static final boolean IS_CRASHLYTICS_ENABLED = isCrashlyticsEnabled();

    private static final String TAG = "CleverDrawer";

    @Override
    public void onCreate() {
        Timber.Tree tree;
        if (IS_CRASHLYTICS_ENABLED) {
            tree = new CrashlyticsTree(getApplicationContext());
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

        Log.d(TAG, "On a physical Android device, Crashlytics logging enabled");
        return true;
    }

    private static boolean isRunningOnEmulator() {
        // Inspired by
        // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in>
        if (Build.PRODUCT == null) {
            return false;
        }

        Set<String> parts = new HashSet<>(Arrays.asList(Build.PRODUCT.split("_")));
        parts.remove("sdk");
        parts.remove("google");
        parts.remove("x86");
        parts.remove("phone");

        // If the build identifier contains only the above keywords in some order, then we're
        // in an emulator
        return parts.isEmpty();
    }

    private static boolean isRunningOnAndroid() {
        // Inspired by: https://developer.android.com/reference/java/lang/System.html#getProperties()
        // Developed using trial and error...
        final Properties properties = System.getProperties();
        final String httpAgent = (String)properties.get("http.agent");
        if (httpAgent == null) {
            return false;
        }
        return httpAgent.contains("Android");
    }

    private static class CrashlyticsTree extends Timber.Tree {
        public CrashlyticsTree(Context context) {
            Fabric.with(context, new Crashlytics());
        }

        @Override
        protected void log(int priority, String tag, @NonNull String message, Throwable t) {
            if (BuildConfig.DEBUG) {
                tag = "DEBUG";
            } else if (TextUtils.isEmpty(tag)) {
                tag = "CleverDrawer";
            }

            // This call logs to *both* Crashlytics and LogCat, and will log the Exception backtrace
            // to LogCat on exceptions.
            Crashlytics.log(priority, tag, message);

            if (t != null) {
                Crashlytics.logException(t);
            }
        }
    }
}
