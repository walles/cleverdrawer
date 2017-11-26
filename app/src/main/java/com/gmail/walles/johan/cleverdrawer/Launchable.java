package com.gmail.walles.johan.cleverdrawer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.TestOnly;

public class Launchable {
    private ResolveInfo resolveInfo;
    private PackageManager packageManager;

    private Drawable icon;
    private String name;

    public final String id;
    public final Intent launchIntent;

    public Launchable(ResolveInfo resolveInfo, PackageManager packageManager) {
        this.resolveInfo = resolveInfo;
        this.packageManager = packageManager;

        // Fast!
        this.launchIntent = createLaunchIntent(resolveInfo);

        // Fast!
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        this.id = activityInfo.applicationInfo.packageName + "." + activityInfo.name;
    }

    public Drawable getIcon() {
        if (icon == null) {
            // Slow!
            icon = resolveInfo.loadIcon(packageManager);
        }

        return icon;
    }

    @Nullable
    public String getName() {
        if (name != null) {
            return name;
        }

        if (resolveInfo != null) {
            // Slow!
            name = resolveInfo.loadLabel(packageManager).toString();
            return name;
        }

        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    @TestOnly
    public Launchable(String id, @Nullable String name) {
        this.id = id;
        this.name = name;
        this.icon = null;
        this.launchIntent = null;
    }

    private static Intent createLaunchIntent(
            ResolveInfo resolveInfo)
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        ActivityInfo activityInfo = resolveInfo.activityInfo;
        ComponentName componentName =
                new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);

        intent.setComponent(componentName);

        return intent;
    }

    @Override
    public String toString() {
        return id;
    }
}
