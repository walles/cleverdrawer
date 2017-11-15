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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;
import org.sqldroid.DroidDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;
    private final List<Launchable> launchables;

    public LaunchableAdapter(Context context) {
        this.context = context;
        launchables = getLaunchables(context);
        sort(launchables);
    }

    private void sort(List<Launchable> launchables) {
        DroidDataSource dataSource =
                new DroidDataSource(context.getPackageName(), "cleverness");
        ContextHolder.setContext(context);
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();

        Collections.sort(launchables, new Comparator<Launchable>() {
            @Override
            public int compare(Launchable o1, Launchable o2) {
                // FIXME: Before this fallback, sort by database content
                return o1.name.compareTo(o2.name);
            }
        });
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
            assert inflater != null;
            view = inflater.inflate(R.layout.launchable, null, false);
        }

        Launchable launchable = (Launchable)getItem(i);
        TextView textView = view.findViewById(R.id.launchableName);
        textView.setText(launchable.name);
        ImageView imageView = view.findViewById(R.id.launchableIcon);
        imageView.setImageDrawable(launchable.icon);

        return view;
    }

    private static List<Launchable> getLaunchables(Context context) {
        final PackageManager packageManager = context.getPackageManager();

        Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        queryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(queryIntent, 0);

        SortedSet<Launchable> launchables = new TreeSet<>();
        for(ResolveInfo resolveInfo : resInfos) {
            launchables.add(new Launchable(resolveInfo, packageManager));
        }

        return new ArrayList<>(launchables);
    }
}
