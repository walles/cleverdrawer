package com.gmail.walles.johan.cleverdrawer;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        Fabric.with(this, new Crashlytics());
        Timber.plant(new Timber.DebugTree());

        super.onCreate();
    }
}
