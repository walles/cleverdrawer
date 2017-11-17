package com.gmail.walles.johan.cleverdrawer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import org.jetbrains.annotations.TestOnly;

public class Launchable {
    public final String id;
    public final String name;
    public final Drawable icon;
    public final Intent launchIntent;

    public Launchable(ResolveInfo resolveInfo, PackageManager packageManager) {
        this.name = resolveInfo.loadLabel(packageManager).toString();
        this.icon = resolveInfo.loadIcon(packageManager);
        this.launchIntent = createLaunchIntent(resolveInfo);

        ActivityInfo activityInfo = resolveInfo.activityInfo;
        this.id = activityInfo.applicationInfo.packageName + "." + activityInfo.name;
    }

    @TestOnly
    public Launchable(String name) {
        this.id = name;
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
