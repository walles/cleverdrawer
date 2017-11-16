package com.gmail.walles.johan.cleverdrawer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

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

        GridView gridView = view.findViewById(R.id.grid_view);
        gridView.setAdapter(new LaunchableAdapter(getContext(), statistics.getComparator()));
        gridView.setOnItemClickListener((adapterView, view1, position, id) -> {
            Launchable launchable = (Launchable)adapterView.getItemAtPosition(position);
            Timber.i("Launching %s...", launchable.name);
            getContext().startActivity(launchable.launchIntent);
            statistics.update(launchable);
            getActivity().finish();
        });
        return view;
    }
}
