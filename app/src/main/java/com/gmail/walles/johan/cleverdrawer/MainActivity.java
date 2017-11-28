package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
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
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_search) {
            // Toggle the MainActivityFragment search bar
            if (searchBox.getVisibility() == View.VISIBLE) {
                searchBox.setVisibility(View.GONE);

                // Close soft keyboard if open, from: https://stackoverflow.com/a/1109108/473672
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
            } else {
                searchBox.setVisibility(View.VISIBLE);
                searchBox.requestFocus();

                // From: https://stackoverflow.com/a/8991563/473672
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
