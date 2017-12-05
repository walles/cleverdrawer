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
    public static final boolean IS_CRASHLYTICS_ENABLED = isCrashlyticsEnabled();

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
