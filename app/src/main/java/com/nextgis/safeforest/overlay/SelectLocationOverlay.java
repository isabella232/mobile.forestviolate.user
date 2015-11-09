/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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

package com.nextgis.safeforest.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.view.MotionEvent;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.safeforest.R;

public class SelectLocationOverlay extends Overlay implements MapViewEventListener {
    private GeoPoint mMapPoint;
    private OverlayItem mOverlayPoint;
    private boolean mIsLocked;

    public SelectLocationOverlay(Context context, MapViewOverlays mapViewOverlays) {
        super(context, mapViewOverlays);
        mMapViewOverlays.addListener(this);
        mOverlayPoint = new OverlayItem(mMapViewOverlays.getMap(), 0, 0, getMarker());
    }

    protected Bitmap getMarker() {
        float scaledDensity = mContext.getResources().getDisplayMetrics().scaledDensity;
        int size = (int) (16 * scaledDensity);
        Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(marker);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        //noinspection deprecation
        p.setColor(mContext.getResources().getColor(R.color.accent));
        p.setAlpha(192);
        c.drawOval(new RectF(0, 0, size * 3 / 4, size * 3 / 4), p);

        return marker;
    }

    public Location getSelectedLocation() {
        if (mMapPoint == null)
            return null;

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(System.currentTimeMillis());
        GeoPoint point = mOverlayPoint.getCoordinates(GeoConstants.CRS_WGS84);
        location.setLatitude(point.getX());
        location.setLongitude(point.getY());

        return location;
    }

    @Override
    public void draw(Canvas canvas, MapDrawable mapDrawable) {
        if (mMapPoint != null) {
            mOverlayPoint.setCoordinates(mMapPoint.getX(), mMapPoint.getY());
            drawOverlayItem(canvas, mOverlayPoint);
        }
    }

    @Override
    public void drawOnPanning(Canvas canvas, PointF currentMouseOffset) {
        drawOnPanning(canvas, currentMouseOffset, mOverlayPoint);
    }

    @Override
    public void drawOnZooming(Canvas canvas, PointF currentFocusLocation, float scale) {
        drawOnZooming(canvas, currentFocusLocation, scale, mOverlayPoint, false);
    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public void onSingleTapUp(MotionEvent event) {
        if (!isVisible() || mIsLocked)
            return;

        mMapPoint = mMapViewOverlays.getMap().screenToMap(new GeoPoint(event.getX(), event.getY()));
        mMapPoint.setCRS(GeoConstants.CRS_WEB_MERCATOR);
        mMapPoint.project(GeoConstants.CRS_WGS84);
        mOverlayPoint.setCoordinates(mMapPoint.getX(), mMapPoint.getY());
        mMapViewOverlays.invalidate();
    }

    @Override
    public void panStart(MotionEvent e) {
        mIsLocked = true;
    }

    @Override
    public void panMoveTo(MotionEvent e) {

    }

    @Override
    public void panStop() {
        mIsLocked = false;
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
