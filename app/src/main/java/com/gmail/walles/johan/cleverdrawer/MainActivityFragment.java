package com.gmail.walles.johan.cleverdrawer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import timber.log.Timber;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
        Timber.d("Main Activity Fragment being constructed...");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listLaunchables();
    }

    private void listLaunchables() {
        final PackageManager packageManager = getContext().getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(intent, 0);

        SortedSet<CharSequence> activityNames = new TreeSet<>();
        for(ResolveInfo resolveInfo : resInfos) {
            activityNames.add(resolveInfo.loadLabel(packageManager));
        }

        for (CharSequence activityName: activityNames) {
            Timber.i("Launchable: %s", activityName);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}
