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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.crashlytics.android.answers.CustomEvent;

import java.io.File;
import java.util.Arrays;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    // See: https://developer.android.com/training/permissions/requesting.html#make-the-request
    private static final int REQUEST_READ_CONTACTS = 29;

    @Nullable
    private LaunchableAdapter launchableAdapter;

    private EditText searchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchBox = findViewById(R.id.searchBox);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_contact_developer) {
            emailDeveloper();
            return true;
        }

        if (id == R.id.action_search) {
            // Toggle the MainActivityFragment search bar
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            if (searchBox.getVisibility() == View.VISIBLE) {
                searchBox.setVisibility(View.GONE);
                searchBox.setText("");

                // Close soft keyboard if open, from: https://stackoverflow.com/a/1109108/473672
                imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
            } else {
                searchBox.setText("");
                searchBox.setVisibility(View.VISIBLE);
                searchBox.requestFocus();

                // From: https://stackoverflow.com/a/8991563/473672
                imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
            }

            return true;
        }

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
