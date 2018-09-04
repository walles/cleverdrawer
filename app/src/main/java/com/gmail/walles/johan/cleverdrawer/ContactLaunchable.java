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
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.gmail.walles.johan.cleverdrawer.utils.CaseInsensitive;
import com.gmail.walles.johan.cleverdrawer.utils.Timer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

class ContactLaunchable extends Launchable {
    private final long id;
    private final Uri photoRef;
    private final Context context;

    // FIXME: Somehow read these as well:
    // * ContactsContract.CommonDataKinds.Organization.COMPANY,
    // * ContactsContract.CommonDataKinds.Nickname.NAME,
    private static final String[] PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
    };

    private ContactLaunchable(Context context, long id, CaseInsensitive name, @Nullable Uri photoRef) {
        super("contacts." + id);
        this.context = context;
        this.id = id;
        this.photoRef = photoRef;
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

            Timer timer = new Timer();
            int idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int photoThumbnailUriColumnIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
            timer.addLeg("scanning");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumnIndex);
                CaseInsensitive name = CaseInsensitive.create(cursor.getString(nameColumnIndex));
                String photoThumbnailUri = cursor.getString(photoThumbnailUriColumnIndex);

                Uri photoRef = null;
                if (photoThumbnailUri != null) {
                    photoRef = Uri.parse(photoThumbnailUri);
                }

                ContactLaunchable contactLaunchable =
                        new ContactLaunchable(context, id, name, photoRef);
                launchables.add(contactLaunchable);
            }

            Timber.i("loading %d contacts timings: %s", launchables.size(), timer);

            return launchables;
        }
    }

    @Override
    public Drawable getIcon() {
        if (photoRef != null) {
            try {
                // This person has a personal photo, use that instead
                InputStream inputStream = context.getContentResolver().openInputStream(photoRef);
                return Drawable.createFromStream(inputStream, photoRef.toString());
            } catch (FileNotFoundException e) {
                Timber.w(e, "Contact photo thumbnail not found: %s", photoRef);
            }
        }

        return ContextCompat.getDrawable(context, R.drawable.head);
    }

    @Override
    public boolean contains(CaseInsensitive substring) {
        if (substring.isEmpty()) {
            return true;
        }

        if (getName().contains(substring)) {
            return true;
        }

        // FIXME: Match against nickname

        // FIXME: Match against organization

        return false;
    }

    @Override
    public double getScoreFactor() {
        // Put contacts after settings, see the same method in IntentLaunchable.java
        return 0.98;
    }

    @Override
    public Intent getLaunchIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
        intent.setData(uri);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        return intent;
    }
}
