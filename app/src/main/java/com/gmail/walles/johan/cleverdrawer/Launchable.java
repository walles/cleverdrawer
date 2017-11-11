package com.gmail.walles.johan.cleverdrawer;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class Launchable implements Comparable<Launchable> {
    public final String name;
    public final Drawable icon;

    public Launchable(String name, Drawable icon) {
        this.name = name;
        this.icon = icon;
    }

    @Override
    public int compareTo(@NonNull Launchable launchable) {
        return name.compareTo(launchable.name);
    }
}
