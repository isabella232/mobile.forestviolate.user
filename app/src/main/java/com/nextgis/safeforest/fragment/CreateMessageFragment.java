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
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.CreateMessageActivity;
import com.nextgis.safeforest.util.Constants;

import java.io.IOException;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class CreateMessageFragment
        extends Fragment
        implements CreateMessageActivity.OnSaveListener, GpsEventListener
{
    protected EditText mMessage;
    protected Location mLocation = null;


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
        View view = inflater.inflate(R.layout.fragment_create_message, null);
        mMessage = (EditText) view.findViewById(R.id.message);

        return view;
    }


    @Override
    public void onPause()
    {
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        if (null != app) {
            GpsEventSource gpsEventSource = app.getGpsEventSource();
            gpsEventSource.removeListener(this);
        }
        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        if (null != app) {
            GpsEventSource gpsEventSource = app.getGpsEventSource();
            gpsEventSource.addListener(this);
        }
    }


    @Override
    public void onSave()
    {
        saveMessage();
    }


    protected void saveMessage()
    {
        if (!com.nextgis.maplib.util.Constants.DEBUG_MODE && null == mLocation) {
            // TODO: do not close activity
            Toast.makeText(getContext(), "none location", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();

        values.put(Constants.FIELD_MDATE, System.currentTimeMillis());
        if(com.nextgis.maplib.util.Constants.DEBUG_MODE) {
            values.put(Constants.FIELD_AUTHOR, "email@email.com");
            values.put(Constants.FIELD_CONTACT, "+79001234567");
        }
        else{
            // TODO: 09.09.15 release this
        }
        values.put(Constants.FIELD_STATUS, Constants.MSG_STATUS_NEW);
        values.put(Constants.FIELD_MTYPE, Constants.MSG_TYPE_FELLING);
        values.put(Constants.FIELD_MESSAGE, mMessage.getText().toString());

        try {
            GeoPoint pt;
            if(com.nextgis.maplib.util.Constants.DEBUG_MODE) {
               pt = new GeoPoint(0, 0);
            }
            else {
                pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
            }
            pt.setCRS(CRS_WGS84);
            pt.project(CRS_WEB_MERCATOR);
            GeoMultiPoint mpt = new GeoMultiPoint();
            mpt.add(pt);
            values.put(FIELD_GEOM, mpt.toBlob());
            Log.d(TAG, "MessageFragment, saveMessage(), pt: " + pt.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }


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


    @Override
    public void onLocationChanged(Location location)
    {
        mLocation = location;
    }


    @Override
    public void onBestLocationChanged(Location location)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }
}
