/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.safeforest;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.overlay.CurrentLocationOverlay;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.util.SettingsConstants;

public class MapFragment
        extends Fragment
        implements MapViewEventListener, GpsEventListener {

    protected MapViewOverlays mMap;
    protected FloatingActionButton mivZoomIn;
    protected FloatingActionButton mivZoomOut;
    protected RelativeLayout mMapRelativeLayout;
    protected View mMainButton;
    protected GpsEventSource mGpsEventSource;
    protected CurrentLocationOverlay mCurrentLocationOverlay;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        MainApplication app = (MainApplication) getActivity().getApplication();

        mMap = new MapViewOverlays(getActivity(), (MapDrawable) app.getMap());
        mMap.setId(999);

        mGpsEventSource = app.getGpsEventSource();
        mCurrentLocationOverlay = new CurrentLocationOverlay(getActivity(), mMap);
        mCurrentLocationOverlay.setStandingMarker(R.drawable.ic_location_standing);
        mCurrentLocationOverlay.setMovingMarker(R.drawable.ic_location_moving);

        mMap.addOverlay(mCurrentLocationOverlay);

        //search relative view of map, if not found - add it
        mMapRelativeLayout = (RelativeLayout) view.findViewById(R.id.maprl);
        if (mMapRelativeLayout != null) {
            mMapRelativeLayout.addView(
                    mMap, 0, new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT));
        }
        mMap.invalidate();

        mMainButton = view.findViewById(R.id.action_addviolation);

        if (null != mMainButton) {
            mMainButton.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {

                    }
                });
        }

        mivZoomIn = (FloatingActionButton) view.findViewById(R.id.action_zoom_in);
        if (null != mivZoomIn) {
            mivZoomIn.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (mivZoomIn.isEnabled()) {
                                mMap.zoomIn();
                            }
                        }
                    });
        }

        mivZoomOut = (FloatingActionButton) view.findViewById(R.id.action_zoom_out);
        if (null != mivZoomOut) {
            mivZoomOut.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (mivZoomOut.isEnabled()) {
                                mMap.zoomOut();
                            }
                        }
                    });
        }

        return view;
    }


    @Override
    public void onDestroyView()
    {
        if (mMap != null) {
            mMap.removeListener(this);
            if (mMapRelativeLayout != null) {
                mMapRelativeLayout.removeView(mMap);
            }
        }

        super.onDestroyView();
    }


    protected void showMapButtons(
            boolean show,
            RelativeLayout rl)
    {
        if (null == rl) {
            return;
        }
        View v = rl.findViewById(R.id.action_zoom_out);
        if (null != v) {
            if (show) {
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }

        v = rl.findViewById(R.id.action_zoom_in);
        if (null != v) {
            if (show) {
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }

        rl.invalidate();
    }

    @Override
    public void onPause()
    {
        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.stopShowingCurrentLocation();
        }
        if (null != mGpsEventSource) {
            mGpsEventSource.removeListener(this);
        }


        final SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        if (null != mMap) {
            edit.putFloat(SettingsConstants.KEY_PREF_ZOOM_LEVEL, mMap.getZoomLevel());
            GeoPoint point = mMap.getMapCenter();
            edit.putLong(SettingsConstants.KEY_PREF_SCROLL_X, Double.doubleToRawLongBits(point.getX()));
            edit.putLong(SettingsConstants.KEY_PREF_SCROLL_Y, Double.doubleToRawLongBits(point.getY()));

            mMap.removeListener(this);
        }
        edit.commit();

        super.onPause();
    }


    @Override
    public void onResume()
    {
        super.onResume();

        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());

        boolean showControls = prefs.getBoolean(SettingsConstants.KEY_PREF_SHOW_ZOOM_CONTROLS, false);
        showMapButtons(showControls, mMapRelativeLayout);

        Log.d(Constants.TAG, "KEY_PREF_SHOW_ZOOM_CONTROLS: " + (showControls ? "ON" : "OFF"));

        if (null != mMap) {
            float mMapZoom;
            try {
                mMapZoom = prefs.getFloat(SettingsConstants.KEY_PREF_ZOOM_LEVEL, mMap.getMinZoom());
            } catch (ClassCastException e) {
                mMapZoom = mMap.getMinZoom();
            }

            double mMapScrollX;
            double mMapScrollY;
            try {
                mMapScrollX = Double.longBitsToDouble(prefs.getLong(SettingsConstants.KEY_PREF_SCROLL_X, 0));
                mMapScrollY = Double.longBitsToDouble(prefs.getLong(SettingsConstants.KEY_PREF_SCROLL_Y, 0));
            } catch (ClassCastException e) {
                mMapScrollX = 0;
                mMapScrollY = 0;
            }
            mMap.setZoomAndCenter(mMapZoom, new GeoPoint(mMapScrollX, mMapScrollY));

            mMap.addListener(this);
        }

        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.updateMode(
                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getString(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC, "3"));
            mCurrentLocationOverlay.startShowingCurrentLocation();
        }
        if (null != mGpsEventSource) {
            mGpsEventSource.addListener(this);
        }
    }

    public void refresh()
    {
        if (null != mMap) {
            mMap.drawMapDrawable();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public void onSingleTapUp(MotionEvent event) {

    }

    @Override
    public void panStart(MotionEvent e) {

    }

    @Override
    public void panMoveTo(MotionEvent e) {

    }

    @Override
    public void panStop() {

    }

    @Override
    public void onLayerAdded(int id) {

    }

    @Override
    public void onLayerDeleted(int id) {

    }

    @Override
    public void onLayerChanged(int id) {

    }

    @Override
    public void onExtentChanged(float zoom, GeoPoint center) {

    }

    @Override
    public void onLayersReordered() {

    }

    @Override
    public void onLayerDrawFinished(int id, float percent) {

    }

    @Override
    public void onLayerDrawStarted() {

    }
}
