package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;
    private final List<CharSequence> launchables;

    public LaunchableAdapter(Context context) {
        this.context = context;
        launchables = getLaunchables(context);
    }

    @Override
    public int getCount() {
        return launchables.size();
    }

    @Override
    public Object getItem(int i) {
        return launchables.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TextView view;
        if (convertView != null) {
            view = (TextView)convertView;
        } else {
            view = new TextView(context);
        }

        view.setText(launchables.get(i));

        return view;
    }

    private static List<CharSequence> getLaunchables(Context context) {
        final PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(intent, 0);

        SortedSet<CharSequence> activityNames = new TreeSet<>();
        for(ResolveInfo resolveInfo : resInfos) {
            activityNames.add(resolveInfo.loadLabel(packageManager));
        }

        return new ArrayList<>(activityNames);
    }
}
