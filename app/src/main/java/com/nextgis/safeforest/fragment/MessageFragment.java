/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.safeforest.fragment;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.MessageActivity;
import com.nextgis.safeforest.util.Constants;

import static com.nextgis.maplib.util.Constants.TAG;


public class MessageFragment
        extends Fragment
        implements MessageActivity.OnSaveListener
{
    protected EditText mMessage;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_message, null);
        mMessage = (EditText) view.findViewById(R.id.message);

        return view;
    }


    @Override
    public void onSave()
    {
        saveMessage();
    }


    protected void saveMessage()
    {
        ContentValues values = new ContentValues();

        values.put(Constants.FIELD_MDATE, System.currentTimeMillis());
        values.put(Constants.FIELD_AUTHOR, "email@email.com");
        values.put(Constants.FIELD_CONTACT, "+79001234567");
        values.put(Constants.FIELD_STATUS, Constants.MSG_STATUS_NEW);
        values.put(Constants.FIELD_MTYPE, Constants.MSG_TYPE_LOGGING);
        values.put(Constants.FIELD_MESSAGE, mMessage.getText().toString());


        final MainApplication app = (MainApplication) getActivity().getApplication();
        Uri uri =
                Uri.parse("content://" + app.getAuthority() + "/" + Constants.KEY_CITIZEN_MESSAGES);
        Uri result = app.getContentResolver().insert(uri, values);

        if (result == null) {
            Log.d(
                    TAG, "MessageFragment, saveMessage(), Layer: " +
                         Constants.KEY_CITIZEN_MESSAGES + ", insert FAILED");
            Toast.makeText(app, R.string.error_create_message, Toast.LENGTH_LONG).show();

        } else {
            long id = Long.parseLong(result.getLastPathSegment());
            Log.d(
                    TAG, "MessageFragment, saveMessage(), Layer: " +
                         Constants.KEY_CITIZEN_MESSAGES + ", id: " +
                         id + ", insert result: " + result);
        }
    }
}
