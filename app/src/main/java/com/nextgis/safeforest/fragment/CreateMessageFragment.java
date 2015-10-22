/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
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
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.IMessage;

import java.io.IOException;
import java.text.DecimalFormat;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class CreateMessageFragment
        extends Fragment
        implements IMessage, GpsEventListener
{
    protected EditText mMessage;
    protected Location mLocation = null;

    protected TextView mLatView;
    protected TextView mLongView;
    protected TextView mAltView;
    protected TextView mAccView;

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
        View view = inflater.inflate(R.layout.fragment_create_message, container, false);
        mMessage = (EditText) view.findViewById(R.id.message);
        mLatView = (TextView) getActivity().findViewById(R.id.latitude_view);
        mLongView = (TextView) getActivity().findViewById(R.id.longitude_view);
        mAltView = (TextView) getActivity().findViewById(R.id.altitude_view);
        mAccView = (TextView) getActivity().findViewById(R.id.accuracy_view);
        setLocationText(null);

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
    public void onLocationChanged(Location location) {
        setLocationText(mLocation);
    }


    @Override
    public void onBestLocationChanged(Location location) {
    }


    @Override
    public void onGpsStatusChanged(int event) {
    }


    protected void setLocationText(Location location)
    {
        mLocation = location;

        if (null == mLatView || null == mLongView || null == mAccView || null == mAltView) {
            return;
        }

        String latAppendix, longAppendix, altAppendix, accAppendix;
        if (null == location) {
            latAppendix = longAppendix = altAppendix = accAppendix = getString(R.string.n_a);
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            int nFormat = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
            DecimalFormat df = new DecimalFormat("0.0");

            latAppendix = LocationUtil.formatLatitude(location.getLatitude(), nFormat, getResources());
            longAppendix = LocationUtil.formatLongitude(location.getLongitude(), nFormat, getResources());
            altAppendix = df.format(location.getAltitude()) + " " + getString(R.string.unit_meter);
            accAppendix = df.format(location.getAccuracy()) + " " + getString(R.string.unit_meter);
        }

        mLatView.setText(getLocationText(R.string.latitude_caption_short, latAppendix));
        mLongView.setText(getLocationText(R.string.longitude_caption_short, longAppendix));
        mAltView.setText(getLocationText(R.string.altitude_caption_short, altAppendix));
        mAccView.setText(getLocationText(R.string.accuracy_caption_short, accAppendix));
    }

    protected String getLocationText(int itemResId, String value) {
        return getString(itemResId) + value;
    }

    @Override
    public boolean isValidData() {
        return mLocation != null;
    }
    @Override
    public int getErrorToastResId() {
        return isValidData() ? 0 : R.string.error_no_location;
    }

    @Override
    public ContentValues getMessageData() {
        ContentValues values = new ContentValues();

        values.put(Constants.FIELD_MDATE, mLocation.getTime());
        values.put(Constants.FIELD_MESSAGE, mMessage.getText().toString());

        try {
            GeoPoint pt;
            if (com.nextgis.maplib.util.Constants.DEBUG_MODE) {
                pt = new GeoPoint(0, 0);
            } else {
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

        return values;
    }
}
