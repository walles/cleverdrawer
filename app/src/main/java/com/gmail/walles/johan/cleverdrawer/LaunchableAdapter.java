package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;
    private final List<Launchable> launchables;

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
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater =
                    (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.launchable, null);
        }

        Launchable launchable = (Launchable)getItem(i);
        TextView textView = (TextView)view.findViewById(R.id.launchableName);
        textView.setText(launchable.name);
        ImageView imageView = (ImageView)view.findViewById(R.id.launchableIcon);
        imageView.setImageDrawable(launchable.icon);

        return view;
    }

    private static List<Launchable> getLaunchables(Context context) {
        final PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(intent, 0);

        SortedSet<Launchable> launchables = new TreeSet<>();
        for(ResolveInfo resolveInfo : resInfos) {
            Launchable launchable =
                    new Launchable(
                            resolveInfo.loadLabel(packageManager).toString(),
                            resolveInfo.loadIcon(packageManager));
            launchables.add(launchable);
        }

        return new ArrayList<>(launchables);
    }
}
