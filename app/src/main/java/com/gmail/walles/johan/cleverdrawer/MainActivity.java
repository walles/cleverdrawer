/*
 * MIT License
 *
 * Copyright (c) 2017 Johan Walles <johan.walles@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.gmail.walles.johan.cleverdrawer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.GridView;

import com.crashlytics.android.answers.CustomEvent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    // See: https://developer.android.com/training/permissions/requesting.html#make-the-request
    private static final int REQUEST_READ_CONTACTS = 29;

    @Nullable
    private LaunchableAdapter launchableAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timer timer = new Timer();
        timer.addLeg("Inflating View");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final File launchHistoryFile = MainActivity.getLaunchHistoryFile(this);
        final File cacheFile = new File(getFilesDir(), "nameCache.json");

        timer.addLeg("Finding GridView");
        GridView gridView = findViewById(R.id.iconGrid);
        timer.addLeg("Constructing Adapter");
        LaunchableAdapter adapter =
                new LaunchableAdapter(this, launchHistoryFile, cacheFile);
        gridView.setAdapter(adapter);
        timer.addLeg("Setting up Listener");
        gridView.setOnItemClickListener((adapterView, view1, position, id) -> {
            Launchable launchable = (Launchable)adapterView.getItemAtPosition(position);

            Timber.i("Launching %s (%s)...", launchable.getName(), launchable.getId());
            try {
                startActivity(launchable.getLaunchIntent());

                DatabaseUtils.registerLaunch(launchHistoryFile, launchable);

                LoggingUtils.logCustom(new CustomEvent("Launched Other App"));
            } catch (RuntimeException e) {
                // We can get a SecurityException, log what we were trying to launch. Note that the
                // above info level message with this information never seems to reach Crashlytics.
                Timber.e(e, "Failed to launch %s (%s)", launchable.getName(), launchable.getId());
            } catch (IOException e) {
                Timber.w(e, "Failed to register %s launch: %s", launchable.getName(), launchable.getId());
            }

            finish();
        });

        timer.addLeg("Finding Search Box");
        EditText searchBox = findViewById(R.id.searchBox);
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            // We already have it
            return;
        }

        // FIXME: Show explanation as described here:
        // https://developer.android.com/training/permissions/requesting.html#make-the-request

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_READ_CONTACTS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_contact_developer) {
            emailDeveloper();
            return true;
        }

        if (id == R.id.action_view_source_code) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/walles/cleverdrawer?files=1"));
            startActivity(intent);
            return true;
        }

        Timber.w("Got unrecognized options item ID %d", id);
        return super.onOptionsItemSelected(item);
    }

    /**
     * Compose an e-mail with version number and the launch history file attached.
     */
    private void emailDeveloper() {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("message/rfc822");

        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "johan.walles@gmail.com" });

        intent.putExtra(Intent.EXTRA_SUBJECT, "CleverDrawer " + BuildConfig.VERSION_NAME);

        File launchHistoryFile = getLaunchHistoryFile(this);
        if (launchHistoryFile.isFile()) {
            Uri launchHistoryUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    launchHistoryFile);
            intent.putExtra(Intent.EXTRA_STREAM, launchHistoryUri);
        }

        startActivity(Intent.createChooser(intent, "Contact Developer"));
    }

    public static File getLaunchHistoryFile(Context context) {
        return new File(context.getFilesDir(), "statistics.json");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_CONTACTS: {
                String response;
                if (grantResults.length == 0) {
                    // If request is cancelled, the result arrays are empty.
                    response = "Cancel";
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    response = "Allow";
                    if (launchableAdapter != null) {
                        launchableAdapter.reloadLaunchables();
                    }
                } else {
                    response = "Deny";
                }

                LoggingUtils.logCustom(new CustomEvent("Request READ_CONTACTS")
                        .putCustomAttribute("Response", response));

                return;
            }

            default: {
                Timber.w("Got unknown permissions result %d: %s", requestCode, Arrays.toString(permissions));
            }
        }
    }

    public void setLaunchableAdapter(@Nullable LaunchableAdapter launchableAdapter) {
        this.launchableAdapter = launchableAdapter;
    }
}
