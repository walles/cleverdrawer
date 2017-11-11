package com.gmail.walles.johan.cleverdrawer;

import android.support.annotation.NonNull;

public class Launchable implements Comparable<Launchable> {
    public final String name;

    public Launchable(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(@NonNull Launchable launchable) {
        return name.compareTo(launchable.name);
    }
}
