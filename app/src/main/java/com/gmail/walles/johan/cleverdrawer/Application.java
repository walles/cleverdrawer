package com.gmail.walles.johan.cleverdrawer;

import timber.log.Timber;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        Timber.plant(new Timber.DebugTree());

        super.onCreate();
    }
}
