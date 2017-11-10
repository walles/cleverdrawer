package com.gmail.walles.johan.cleverdrawer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class LaunchableAdapter extends BaseAdapter {
    private final Context context;

    public LaunchableAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return FILLING.length;
    }

    @Override
    public Object getItem(int i) {
        return FILLING[i];
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

        view.setText(FILLING[i]);

        return view;
    }

    private static String[] FILLING = {
            "Adam", "Bertil", "Caesar", "David", "Goliat"
    };
}
