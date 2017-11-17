package com.gmail.walles.johan.cleverdrawer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.sql.SQLException;
import java.util.Comparator;

import timber.log.Timber;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    public MainActivityFragment() {
        Timber.d("Main Activity Fragment being constructed...");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        final Statistics statistics = new Statistics(getContext());

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        Comparator<Launchable> comparator = null;
        try {
            comparator = statistics.getComparator();
        } catch (SQLException e) {
            throw new RuntimeException("Getting comparator failed", e);
        }

        GridView gridView = view.findViewById(R.id.grid_view);
        gridView.setAdapter(new LaunchableAdapter(getContext(), comparator));
        gridView.setOnItemClickListener((adapterView, view1, position, id) -> {
            Launchable launchable = (Launchable)adapterView.getItemAtPosition(position);
            Timber.i("Launching %s...", launchable.name);
            getContext().startActivity(launchable.launchIntent);
            try {
                statistics.registerLaunch(launchable);
            } catch (SQLException e) {
                Timber.e(e, "Failed to register " + launchable.name + " launch: " + launchable.id);
            }
            getActivity().finish();
        });
        return view;
    }
}
