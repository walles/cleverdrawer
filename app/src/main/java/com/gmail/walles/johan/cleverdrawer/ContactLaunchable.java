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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class ContactLaunchable extends Launchable {
    // FIXME: Somehow read these as well:
    // * ContactsContract.CommonDataKinds.Organization.COMPANY,
    // * ContactsContract.CommonDataKinds.Nickname.NAME,
    private static final String[] PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
    };

    private ContactLaunchable(String id, String name) {
        super("contacts." + id);
        setName(name);
    }

    public static Collection<Launchable> loadLaunchables(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try (Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                PROJECTION, null, null, null))
        {
            if (cursor == null) {
                return Collections.emptyList();
            }

            List<Launchable> launchables = new LinkedList<>();

            int idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            while (cursor.moveToNext()) {
                String id = cursor.getString(idColumnIndex);
                String name = cursor.getString(nameColumnIndex);

                launchables.add(new ContactLaunchable(id, name));
            }

            return launchables;
        }
    }

    @Override
    public Drawable getIcon() {
        // FIXME: Get some image for this person
        return null;
    }

    @Override
    public boolean matches(CharSequence search) {
        // FIXME: Actually match some things
        return true;
    }

    @Override
    public Intent getLaunchIntent() {
        // FIXME: Launch Contacts with this person
        return null;
    }
}
