package com.gmail.walles.johan.cleverdrawer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;

import java.io.File;
import java.io.IOException;

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
        final File statsFile = new File(getContext().getFilesDir(), "statistics.json");
        final File cacheFile = new File(getContext().getFilesDir(), "nameCache.json");

        Timer timer = new Timer();
        timer.addLeg("Inflating View");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        timer.addLeg("Finding GridView");
        GridView gridView = view.findViewById(R.id.iconGrid);
        timer.addLeg("Constructing Adapter");
        LaunchableAdapter adapter = new LaunchableAdapter(getContext(), statsFile, cacheFile);
        gridView.setAdapter(adapter);
        timer.addLeg("Setting up Listener");
        gridView.setOnItemClickListener((adapterView, view1, position, id) -> {
            Launchable launchable = (Launchable)adapterView.getItemAtPosition(position);
            Timber.i("Launching %s (%s)...", launchable.getName(), launchable.id);
            getContext().startActivity(launchable.launchIntent);
            try {
                DatabaseUtils.registerLaunch(statsFile, launchable);
            } catch (IOException e) {
                Timber.e(e, "Failed to register " + launchable.getName() + " launch: " + launchable.id);
            }
            getActivity().finish();
        });

        timer.addLeg("Finding Search Box");
        EditText searchBox = view.findViewById(R.id.searchBox);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This block intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This block intentionally left blank
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.setFilter(s);
            }
        });

        Timber.i("onCreateView() timings: %s", timer.toString());

        return view;
    }
}
