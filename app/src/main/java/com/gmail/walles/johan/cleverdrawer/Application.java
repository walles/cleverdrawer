package com.gmail.walles.johan.cleverdrawer;

import android.annotation.SuppressLint;
import android.os.Build;
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
    private static final String TAG = "CleverDrawer";

    @Override
    public void onCreate() {
        if (isCrashlyticsEnabled()) {
            Fabric.with(this, new Crashlytics());
        }
        Timber.plant(new Timber.DebugTree());

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
}
