/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
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

package com.nextgis.safeforest.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.view.MotionEvent;
import android.widget.Toast;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.api.DrawItem;
import com.nextgis.maplibui.api.MapViewEventListener;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.mapui.MapViewOverlays;
import com.nextgis.maplibui.util.ConstantsUI;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.safeforest.R;

public class SelectLocationOverlay extends Overlay implements MapViewEventListener {
    protected final static int VERTEX_RADIUS = 20;

    protected GeoPoint mSelectedPoint, mCenter;
    protected DrawItem mSelectedItem;
    protected PointF mTempPointOffset;

    protected int mFillColor, mOutlineColor;
    protected Paint mPaint;
    protected final Bitmap mAnchor;
    protected final float mAnchorRectOffsetX, mAnchorRectOffsetY;
    protected final float mAnchorCenterX, mAnchorCenterY;
    protected final float mTolerancePX, mAnchorTolerancePX;

    public SelectLocationOverlay(Context context, MapViewOverlays mapViewOverlays) {
        super(context, mapViewOverlays);
        mMapViewOverlays.addListener(this);

        mCenter = mMapViewOverlays.getMap().getFullScreenBounds().getCenter();
        float[] geoPoints = new float[2];
        geoPoints[0] = (float) mCenter.getX();
        geoPoints[1] = (float) mCenter.getY();
        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, geoPoints);
        fillGeometry();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mFillColor = ControlHelper.getColor(mContext, com.nextgis.maplibui.R.attr.colorPrimary);
        mOutlineColor = ControlHelper.getColor(mContext, com.nextgis.maplibui.R.attr.colorPrimaryDark);

        mAnchor = BitmapFactory.decodeResource(mContext.getResources(), com.nextgis.maplibui.R.drawable.ic_action_anchor);
        mAnchorRectOffsetX = -mAnchor.getWidth() * 0.05f;
        mAnchorRectOffsetY = -mAnchor.getHeight() * 0.05f;
        mAnchorCenterX = mAnchor.getWidth() * 0.75f;
        mAnchorCenterY = mAnchor.getHeight() * 0.75f;
        mAnchorTolerancePX = mAnchor.getScaledWidth(context.getResources().getDisplayMetrics());
        mTolerancePX = context.getResources().getDisplayMetrics().density * ConstantsUI.TOLERANCE_DP;
    }

    public void centerSelectedLocation() {
        mSelectedItem.setSelectedPointCoordinates((float) mCenter.getX(), (float) mCenter.getY());
        fillGeometry();
    }

    public void setSelectedLocation(Location location) {
        if (location == null) {
            Toast.makeText(mContext, R.string.error_no_location, Toast.LENGTH_SHORT).show();
            return;
        }

        mSelectedPoint.setCoordinates(location.getLongitude(), location.getLatitude());
        mSelectedPoint.setCRS(GeoConstants.CRS_WGS84);
        mSelectedPoint.project(GeoConstants.CRS_WEB_MERCATOR);
        mMapViewOverlays.postInvalidate();
    }

    public Location getSelectedLocation() {
        mSelectedPoint.project(GeoConstants.CRS_WGS84);
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(System.currentTimeMillis());
        location.setLongitude(mSelectedPoint.getX());
        location.setLatitude(mSelectedPoint.getY());

        return location;
    }

    protected float[] mapToScreen() {
        return mMapViewOverlays.getMap().mapToScreen(new GeoPoint[]{mSelectedPoint});
    }


    protected GeoPoint screenToMap() {
        return mMapViewOverlays.getMap().screenToMap(mSelectedItem.getSelectedRing())[0];
    }

    @Override
    public void draw(Canvas canvas, MapDrawable mapDrawable) {
        fillDrawItem();
        drawPoint(mSelectedItem, canvas);
    }

    @Override
    public void drawOnPanning(Canvas canvas, PointF currentMouseOffset) {
        DrawItem drawItem = mSelectedItem;
        if (!mMapViewOverlays.isLockMap())
            drawItem = mSelectedItem.pan(currentMouseOffset);

        drawPoint(drawItem, canvas);
    }

    @Override
    public void drawOnZooming(Canvas canvas, PointF currentFocusLocation, float scale) {
        DrawItem drawItem = mSelectedItem.zoom(currentFocusLocation, scale);
        drawPoint(drawItem, canvas);
    }

    private void drawPoint(DrawItem drawItem, Canvas canvas) {
        float[] items = drawItem.getSelectedRing();
        if (null != items) {
            mPaint.setColor(mOutlineColor);
            mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
            canvas.drawPoints(items, mPaint);

            mPaint.setColor(mFillColor);
            mPaint.setStrokeWidth(VERTEX_RADIUS);
            canvas.drawPoints(items, mPaint);

            float anchorX = items[0] + mAnchorRectOffsetX;
            float anchorY = items[1] + mAnchorRectOffsetY;
            canvas.drawBitmap(mAnchor, anchorX, anchorY, null);
        }
    }

    @Override
    public void onLongPress(MotionEvent event) {

    }

    @Override
    public void onSingleTapUp(MotionEvent event) {

    }

    @Override
    public void panStart(MotionEvent event) {
        double dMinX = event.getX() - mTolerancePX * 2 - mAnchorTolerancePX;
        double dMaxX = event.getX() + mTolerancePX;
        double dMinY = event.getY() - mTolerancePX * 2 - mAnchorTolerancePX;
        double dMaxY = event.getY() + mTolerancePX;
        GeoEnvelope screenEnv = new GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY);

        if (mSelectedItem.isTapNearSelectedPoint(screenEnv)) {
            PointF tempPoint = mSelectedItem.getSelectedPoint();
            mTempPointOffset = new PointF(tempPoint.x - event.getX(), tempPoint.y - event.getY());
            mMapViewOverlays.setLockMap(true);
        }
    }

    @Override
    public void panMoveTo(MotionEvent e) {
        if (mMapViewOverlays.isLockMap()) {
            mSelectedItem.setSelectedPointCoordinates(e.getX() + mTempPointOffset.x, e.getY() + mTempPointOffset.y);
        }
    }

    @Override
    public void panStop() {
        if (mMapViewOverlays.isLockMap()) {
            mMapViewOverlays.setLockMap(false);
            fillGeometry();
            mMapViewOverlays.postInvalidate();
        }
    }

    protected void fillGeometry() {
        mSelectedPoint = new GeoPoint(screenToMap());
        mSelectedPoint.setCRS(GeoConstants.CRS_WEB_MERCATOR);
    }

    protected void fillDrawItem() {
        mSelectedItem = new DrawItem(DrawItem.TYPE_VERTEX, mapToScreen());
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
