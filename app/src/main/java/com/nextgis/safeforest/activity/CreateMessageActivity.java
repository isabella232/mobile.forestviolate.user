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

package com.nextgis.safeforest.activity;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.fragment.CreateMessageFragment;
import com.nextgis.safeforest.util.Constants;

import java.text.DecimalFormat;


public class CreateMessageActivity
        extends SFActivity
        implements GpsEventListener
{
    protected OnSaveListener mOnSaveListener;

    protected TextView mLatView;
    protected TextView mLongView;
    protected TextView mAltView;
    protected TextView mAccView;

    protected Location mLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_message);
        setToolbar(R.id.main_toolbar);

        final IGISApplication app = (IGISApplication) getApplication();
        createLocationPanelView(app);

        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        CreateMessageFragment createMessageFragment =
                (CreateMessageFragment) fm.findFragmentByTag(Constants.FRAGMENT_CREATE_MESSAGE);

        if (createMessageFragment == null) {
            createMessageFragment = new CreateMessageFragment();
            setOnSaveListener(createMessageFragment);
        }

        ft.replace(
                R.id.create_message_fragment, createMessageFragment,
                Constants.FRAGMENT_CREATE_MESSAGE);
        ft.commit();
    }


    protected void createLocationPanelView(final IGISApplication app)
    {
        mLatView = (TextView) findViewById(R.id.latitude_view);
        mLongView = (TextView) findViewById(R.id.longitude_view);
        mAltView = (TextView) findViewById(R.id.altitude_view);
        mAccView = (TextView) findViewById(R.id.accuracy_view);
        setLocationText(null);
    }


    @Override
    protected void onPause()
    {
        if (null != findViewById(R.id.location_panel)) {
            IGISApplication app = (IGISApplication) getApplication();
            if (null != app) {
                GpsEventSource gpsEventSource = app.getGpsEventSource();
                gpsEventSource.removeListener(this);
            }
        }
        super.onPause();
    }


    @Override
    protected void onResume()
    {
        if (null != findViewById(R.id.location_panel)) {
            IGISApplication app = (IGISApplication) getApplication();
            if (null != app) {
                GpsEventSource gpsEventSource = app.getGpsEventSource();
                gpsEventSource.addListener(this);
            }
        }
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.message, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.action_save:
                onSave();
                return true;
            case R.id.action_cancel:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void onSave()
    {
        if (null != mOnSaveListener) {
            mOnSaveListener.onSave();
        }
    }


    public void setOnSaveListener(OnSaveListener listener)
    {
        mOnSaveListener = listener;
    }


    @Override
    public void onLocationChanged(Location location)
    {
        setLocationText(location);
    }


    @Override
    public void onBestLocationChanged(Location location)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    protected void setLocationText(Location location)
    {
        if (null == mLatView || null == mLongView || null == mAccView || null == mAltView) {
            return;
        }

        if (null == location) {

            mLatView.setText(
                    getString(com.nextgis.maplibui.R.string.latitude_caption_short) + ": " +
                    getString(
                            com.nextgis.maplibui.R.string.n_a));
            mLongView.setText(
                    getString(com.nextgis.maplibui.R.string.longitude_caption_short) + ": " +
                    getString(
                            com.nextgis.maplibui.R.string.n_a));
            mAltView.setText(
                    getString(com.nextgis.maplibui.R.string.altitude_caption_short) + ": " +
                    getString(
                            com.nextgis.maplibui.R.string.n_a));
            mAccView.setText(
                    getString(com.nextgis.maplibui.R.string.accuracy_caption_short) + ": " +
                    getString(
                            com.nextgis.maplibui.R.string.n_a));

            return;
        }

        mLocation = location;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int nFormat = prefs.getInt(
                SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
        DecimalFormat df = new DecimalFormat("0.0");

        mLatView.setText(
                getString(com.nextgis.maplibui.R.string.latitude_caption_short) + ": " +
                LocationUtil.formatLatitude(location.getLatitude(), nFormat, getResources()));

        mLongView.setText(
                getString(com.nextgis.maplibui.R.string.longitude_caption_short) + ": " +
                LocationUtil.formatLongitude(location.getLongitude(), nFormat, getResources()));

        double altitude = location.getAltitude();
        mAltView.setText(
                getString(com.nextgis.maplibui.R.string.altitude_caption_short) + ": " +
                df.format(altitude) + " " +
                getString(com.nextgis.maplibui.R.string.unit_meter));

        float accuracy = location.getAccuracy();
        mAccView.setText(
                getString(com.nextgis.maplibui.R.string.accuracy_caption_short) + ": " +
                df.format(accuracy) + " " +
                getString(com.nextgis.maplibui.R.string.unit_meter));
    }


    public interface OnSaveListener
    {
        void onSave();
    }
}
