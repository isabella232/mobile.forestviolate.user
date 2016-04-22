/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2016 NextGIS, info@nextgis.com
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

import android.accounts.Account;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.overlay.CurrentLocationOverlay;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.MainActivity;
import com.nextgis.safeforest.overlay.SelectLocationOverlay;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.MapUtil;
import com.nextgis.safeforest.util.SettingsConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapFragment
        extends Fragment
        implements MapViewEventListener, GpsEventListener {

    protected MainApplication mApp;
    protected MapViewOverlays mMap;
    protected FloatingActionButton mivZoomIn;
    protected FloatingActionButton mivZoomOut;
    protected RelativeLayout mMapRelativeLayout;

    protected TextView mStatusSource, mStatusAccuracy, mStatusSpeed, mStatusAltitude,
            mStatusLatitude, mStatusLongitude;
    protected FrameLayout mStatusPanel, mLegend;
    protected boolean mShowStatus = true;

    protected GpsEventSource mGpsEventSource;
    protected CurrentLocationOverlay mCurrentLocationOverlay;
    protected SelectLocationOverlay mSelectLocationOverlay;

    protected boolean mShowStatusPanel, mShowSelectLocation;
    protected GeoPoint mCurrentCenter;
    protected int mCoordinatesFormat, mCoordinatesFraction;

    protected float mTolerancePX;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        mTolerancePX = getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mApp = (MainApplication) getActivity().getApplication();

        mMap = new MapViewOverlays(getActivity(), (MapDrawable) mApp.getMap());
        mMap.setId(R.id.map_view);

        mGpsEventSource = mApp.getGpsEventSource();
        mCurrentLocationOverlay = new CurrentLocationOverlay(getActivity(), mMap);
        mCurrentLocationOverlay.setStandingMarker(R.drawable.ic_location_standing);
        mCurrentLocationOverlay.setMovingMarker(R.drawable.ic_location_moving);
        mSelectLocationOverlay = new SelectLocationOverlay(getActivity(), mMap);
        mSelectLocationOverlay.setVisibility(mShowSelectLocation);

        mMap.addOverlay(mSelectLocationOverlay);
        mMap.addOverlay(mCurrentLocationOverlay);

        //search relative view of map, if not found - add it
        mMapRelativeLayout = (RelativeLayout) view.findViewById(R.id.maprl);
        addMap();

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

        mStatusPanel = (FrameLayout) view.findViewById(R.id.fl_status_panel);
        mLegend = (FrameLayout) view.findViewById(R.id.fl_legend);
        fitLegend();

        return view;
    }

    public void setLegendVisible(boolean visible) {
        if (mLegend != null)
            mLegend.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setStatusVisible(boolean visible) {
        if (mStatusPanel != null)
            mStatusPanel.setVisibility(visible ? View.VISIBLE : View.GONE);

        mShowStatus = visible;
    }

    protected void fitLegend() {
        View content = getActivity().getLayoutInflater().inflate(R.layout.legend_land, mLegend, false);

        if (!isLegendFitsOneLine(content))
            content = getActivity().getLayoutInflater().inflate(R.layout.legend, mLegend, false);

        content.getBackground().setAlpha(128);
        mLegend.removeAllViews();
        mLegend.addView(content);
    }


    protected boolean isLegendFitsOneLine(View content) {
        content.measure(0, 0);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return content.getMeasuredWidth() < metrics.widthPixels;
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

    private void addMap() {
        if (mMapRelativeLayout != null) {
            FrameLayout map = (FrameLayout) mMapRelativeLayout.findViewById(R.id.mapfl);
            map.addView(mMap, 0, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));
        }
    }

    @Override
    public void onPause()
    {
        pauseGps();

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

        Log.d(com.nextgis.maplib.util.Constants.TAG, "KEY_PREF_SHOW_ZOOM_CONTROLS: " + (showControls ? "ON" : "OFF"));

        if (null != mMap) {
            if(prefs.getBoolean(SettingsConstants.KEY_PREF_MAP_FIRST_VIEW, true)){
                //zoom to inspector extent
                float minX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINX, -2000.0f);
                float minY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINY, -2000.0f);
                float maxX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXX, 2000.0f);
                float maxY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXY, 2000.0f);
                mMap.zoomToExtent(new GeoEnvelope(minX, maxX, minY, maxY));

                final SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(SettingsConstants.KEY_PREF_MAP_FIRST_VIEW, false);
                edit.commit();
            }
            else {
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
            }
            mMap.addListener(this);

            setLayers();
        }

        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).isMapShown())
                resumeGps();
        } else
            resumeGps();

        mCoordinatesFormat = prefs.getInt(SettingsConstants.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_DEGREES);
        mCoordinatesFraction = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FRACTION, 6);
        mShowStatusPanel = prefs.getBoolean(SettingsConstantsUI.KEY_PREF_SHOW_STATUS_PANEL, false);

        if (null != mStatusPanel) {
            if (mShowStatusPanel && mShowStatus) {
                mStatusPanel.setVisibility(View.VISIBLE);
                fillStatusPanel(null);
            } else {
                mStatusPanel.removeAllViews();
            }
        }

        mCurrentCenter = null;
    }

    private void setLayers() {
        Account account = mApp.getAccount(getString(R.string.account_name));
        String auth = mApp.getAccountUserData(account, Constants.KEY_IS_AUTHORIZED);
        boolean isAuthorized = auth != null && !auth.equals(Constants.ANONYMOUS);

        Layer layer = (RemoteTMSLayer) mApp.getMap().getLayerByName(getString(R.string.lv));
        if (layer != null)
            layer.setVisible(isAuthorized);

        layer = (RemoteTMSLayer) mApp.getMap().getLayerByName(getString(R.string.ulv));
        if (layer != null)
            layer.setVisible(isAuthorized);

        layer = (RemoteTMSLayer) mApp.getMap().getLayerByName(getString(R.string.geomixer_fv_tiles));
        if (layer != null)
            layer.setVisible(isAuthorized);

        layer = (NGWVectorLayer) mApp.getMap().getLayerByName(Constants.KEY_FV_DOCS);
        if (layer != null) {
            layer.setVisible(isAuthorized);
            int sync = isAuthorized ? com.nextgis.maplib.util.Constants.SYNC_ALL : com.nextgis.maplib.util.Constants.SYNC_NONE;
            ((NGWVectorLayer) layer).setSyncType(sync);
        }

        layer = (RemoteTMSLayer) mApp.getMap().getLayerByName(getString(R.string.fires));
        if (layer != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            ((RemoteTMSLayer) layer).setEndDate(sdf.format(new Date(calendar.getTimeInMillis())));
            calendar.add(Calendar.DAY_OF_MONTH, -2);
            ((RemoteTMSLayer) layer).setStartDate(sdf.format(new Date(calendar.getTimeInMillis())));
        }
    }

    public void pauseGps() {
        if (null != mCurrentLocationOverlay)
            mCurrentLocationOverlay.stopShowingCurrentLocation();

        if (null != mGpsEventSource)
            mGpsEventSource.removeListener(this);
    }

    public void resumeGps() {
        if (null != mCurrentLocationOverlay) {
            mCurrentLocationOverlay.updateMode(PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC, "3"));
            mCurrentLocationOverlay.startShowingCurrentLocation();
        }

        if (null != mGpsEventSource)
            mGpsEventSource.addListener(this);
    }

    public void setZoomAndCenter(float zoom, GeoPoint center) {
        mMap.setZoomAndCenter(zoom, center);
    }

    public void refresh()
    {
        if (null != mMap) {
            mMap.drawMapDrawable();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (mCurrentCenter == null) {
                mCurrentCenter = new GeoPoint();
            }

            mCurrentCenter.setCoordinates(location.getLongitude(), location.getLatitude());
            mCurrentCenter.setCRS(GeoConstants.CRS_WGS84);

            if (!mCurrentCenter.project(GeoConstants.CRS_WEB_MERCATOR)) {
                mCurrentCenter = null;
            }
        }

        fillStatusPanel(location);
    }

    @Override
    public void onBestLocationChanged(Location location) {

    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public void onSingleTapUp(MotionEvent event) {
        selectGeometryInScreenCoordinates(event.getX(), event.getY());
    }

    public void selectGeometryInScreenCoordinates(float x, float y) {
        double dMinX = x - mTolerancePX;
        double dMaxX = x + mTolerancePX;
        double dMinY = y - mTolerancePX;
        double dMaxY = y + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

        GeoEnvelope mapEnv = mMap.screenToMap(screenEnv);
        if (null == mapEnv)
            return;

        List<Long> items;
        VectorLayer layerFV = (VectorLayer) mApp.getMap().getLayerByName(Constants.KEY_FV_FOREST);
        VectorLayer layerMessages = (VectorLayer) mApp.getMap().getLayerByName(Constants.KEY_CITIZEN_MESSAGES);

        if (null != layerFV) {
            items = layerFV.query(mapEnv);
            if (!items.isEmpty()) {
                Feature feature = layerFV.getFeature(items.get(0));
                showForestViolateFeatureDialog(feature);
                return;
            }
        }

        if (null != layerMessages) {
            items = layerMessages.query(mapEnv);
            if (!items.isEmpty()) {
                Feature feature = layerMessages.getFeature(items.get(0));
                showCitizenMessageFeatureDialog(feature);
            }
        }
    }

    private void showForestViolateFeatureDialog(Feature feature) {
        Date date = (Date) feature.getFieldValue(Constants.FIELD_FV_DATE);
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
        String message = format.format(date) + "\r\n\r\n";
        message += getString(R.string.region) + ": " + feature.getFieldValue(Constants.FIELD_FV_REGION) + "\r\n";
        message += getString(R.string.forestry) + ": " + feature.getFieldValue(Constants.FIELD_FV_FORESTRY) + "\r\n";
        message += getString(R.string.precinct) + ": " + feature.getFieldValue(Constants.FIELD_FV_PRECINCT) + "\r\n";
        message += getString(R.string.territory) + ": " + feature.getFieldValue(Constants.FIELD_FV_TERRITORY) + "\r\n\r\n";
        message += feature.getFieldValue(Constants.FIELD_FV_STATUS);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AppCompatDialog);
        dialog.setMessage(message)
                .setTitle("#" + feature.getId())
                .setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }

    private void showCitizenMessageFeatureDialog(Feature feature) {
        Date date = (Date) feature.getFieldValue(Constants.FIELD_MDATE);
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
        String message = format.format(date) + "\r\n\r\n";
        message += getString(R.string.type_paragraph) + ": " + feature.getFieldValue(Constants.FIELD_MTYPE_STR) + "\r\n";
        String status = MapUtil.getStatus(getContext(), ((Long) feature.getFieldValue(Constants.FIELD_STATUS)).intValue());
        message += getString(R.string.status_paragraph) + ": " + status + "\r\n";
        message += getString(R.string.message_paragraph) + ": " + feature.getFieldValue(Constants.FIELD_MESSAGE) + "\r\n\r\n";
        message += feature.getFieldValue(Constants.FIELD_STMESSAGE);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AppCompatDialog);
        dialog.setMessage(message)
                .setTitle("#" + feature.getId())
                .setPositiveButton(android.R.string.ok, null);
        dialog.show();
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
        setZoomInEnabled(mMap.canZoomIn());
        setZoomOutEnabled(mMap.canZoomOut());
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


    protected void setZoomInEnabled(boolean bEnabled)
    {
        if (mivZoomIn == null) {
            return;
        }

        mivZoomIn.setEnabled(bEnabled);
    }


    protected void setZoomOutEnabled(boolean bEnabled)
    {
        if (mivZoomOut == null) {
            return;
        }
        mivZoomOut.setEnabled(bEnabled);
    }

    protected void fillStatusPanel(Location location)
    {
        if (!mShowStatusPanel) //mStatusPanel.getVisibility() == FrameLayout.INVISIBLE)
        {
            return;
        }

        boolean needViewUpdate = true;
        boolean isCurrentOrientationOneLine =
                mStatusPanel.getChildCount() > 0 && ((LinearLayout) mStatusPanel.getChildAt(
                        0)).getOrientation() == LinearLayout.HORIZONTAL;

        View panel;
        if (!isCurrentOrientationOneLine) {
            panel = getActivity().getLayoutInflater()
                    .inflate(R.layout.status_panel_land, mStatusPanel, false);
            defineTextViews(panel);
        } else {
            panel = mStatusPanel.getChildAt(0);
            needViewUpdate = false;
        }

        fillTextViews(location);

        if (!isFitOneLine()) {
            panel = getActivity().getLayoutInflater()
                    .inflate(R.layout.status_panel, mStatusPanel, false);
            defineTextViews(panel);
            fillTextViews(location);
            needViewUpdate = true;
        }

        if (needViewUpdate) {
            mStatusPanel.removeAllViews();
            panel.getBackground().setAlpha(128);
            mStatusPanel.addView(panel);
        }
    }


    protected void fillTextViews(Location location)
    {
        if (null == location) {
            setDefaultTextViews();
        } else {
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                String text = "";
                int satellites = location.getExtras() != null ? location.getExtras().getInt("satellites") : 0;
                if (satellites > 0)
                    text += satellites;

                mStatusSource.setText(text);
                mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_location), null, null, null);
            } else {
                mStatusSource.setText("");
                mStatusSource.setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_signal_wifi), null, null, null);
            }

            mStatusAccuracy.setText(
                    String.format(
                            "%.1f %s", location.getAccuracy(), getString(R.string.unit_meter)));
            mStatusAltitude.setText(
                    String.format(
                            "%.1f %s", location.getAltitude(), getString(R.string.unit_meter)));
            mStatusSpeed.setText(
                    String.format(
                            "%.1f %s/%s", location.getSpeed() * 3600 / 1000,
                            getString(R.string.unit_kilometer), getString(R.string.unit_hour)));
            mStatusLatitude.setText(
                    formatCoordinate(location.getLatitude(), R.string.latitude_caption_short));
            mStatusLongitude.setText(
                    formatCoordinate(location.getLongitude(), R.string.longitude_caption_short));
        }
    }

    private String formatCoordinate(double value, int appendix) {
        return LocationUtil.formatCoordinate(value, mCoordinatesFormat, mCoordinatesFraction) + " " + getString(appendix);
    }

    protected void setDefaultTextViews()
    {
        mStatusSource.setCompoundDrawables(null, null, null, null);
        mStatusSource.setText("");
        mStatusAccuracy.setText(getString(R.string.n_a));
        mStatusAltitude.setText(getString(R.string.n_a));
        mStatusSpeed.setText(getString(R.string.n_a));
        mStatusLatitude.setText(getString(R.string.n_a));
        mStatusLongitude.setText(getString(R.string.n_a));
    }


    protected boolean isFitOneLine()
    {
        mStatusLongitude.measure(0, 0);
        mStatusLatitude.measure(0, 0);
        mStatusAltitude.measure(0, 0);
        mStatusSpeed.measure(0, 0);
        mStatusAccuracy.measure(0, 0);
        mStatusSource.measure(0, 0);

        int totalWidth = mStatusSource.getMeasuredWidth() + mStatusLongitude.getMeasuredWidth() +
                mStatusLatitude.getMeasuredWidth() + mStatusAccuracy.getMeasuredWidth() +
                mStatusSpeed.getMeasuredWidth() + mStatusAltitude.getMeasuredWidth();

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return totalWidth < metrics.widthPixels;
    }


    private void defineTextViews(View panel)
    {
        mStatusSource = (TextView) panel.findViewById(R.id.tv_source);
        mStatusAccuracy = (TextView) panel.findViewById(R.id.tv_accuracy);
        mStatusSpeed = (TextView) panel.findViewById(R.id.tv_speed);
        mStatusAltitude = (TextView) panel.findViewById(R.id.tv_altitude);
        mStatusLatitude = (TextView) panel.findViewById(R.id.tv_latitude);
        mStatusLongitude = (TextView) panel.findViewById(R.id.tv_longitude);
    }

    public void locateCurrentPosition()
    {
        if (mCurrentCenter != null) {
            mMap.panTo(mCurrentCenter);
        } else {
            Toast.makeText(getActivity(), R.string.error_no_location, Toast.LENGTH_SHORT).show();
        }
    }

    public Location getSelectedLocation() {
        return mSelectLocationOverlay.getSelectedLocation();
    }

    public void setSelectedLocation(Location location) {
        mSelectLocationOverlay.setSelectedLocation(location);
    }

    public void setSelectedLocationVisible(boolean isVisible) {
        mShowSelectLocation = isVisible;

        if (mSelectLocationOverlay != null)
            mSelectLocationOverlay.setVisibility(isVisible);
    }

    public void centerSelectedPoint() {
        mSelectLocationOverlay.centerSelectedLocation();
        mMap.postInvalidate();
    }
}
