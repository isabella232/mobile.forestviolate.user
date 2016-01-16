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

package com.nextgis.safeforest.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.nextgis.maplibui.util.CompassImage;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.util.Constants;

public class MessageCompassActivity extends SFActivity implements View.OnClickListener {
    protected CompassImage mBezel, mNeedle;
    protected EditText mDistance;
    protected FloatingActionButton mOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_compass);
        setToolbar(R.id.main_toolbar);

        mBezel = (CompassImage) findViewById(R.id.compass);
        mNeedle = (CompassImage) findViewById(R.id.needle);
        mDistance = (EditText) findViewById(R.id.et_distance);
        mOk = (FloatingActionButton) findViewById(R.id.action_send);
        mOk.setOnClickListener(this);
    }

    protected Location getDistantLocation() {
        Location location = ((MainApplication) getApplication()).getGpsEventSource().getLastKnownLocation();

        // http://www.movable-type.co.uk/scripts/latlong.html#destPoint
        if (location != null) {
            double lat = Math.toRadians(location.getLatitude());
            double lon = Math.toRadians(location.getLongitude());

            String distance = mDistance.getText().toString();
            if (!TextUtils.isEmpty(distance.trim())) {
                double dist = Double.parseDouble(distance);
                dist /= 6371.01 * 1000;

                double bear = mBezel.getAngle() - mNeedle.getAngle();
                bear = Math.toRadians(bear);

                double lat2 = Math.asin(Math.sin(lat) * Math.cos(dist) + Math.cos(lat) * Math.sin(dist) * Math.cos(bear));
                double lon2 = lon + Math.atan2(Math.sin(bear) * Math.sin(dist) * Math.cos(lat), Math.cos(dist) - Math.sin(lat) * Math.sin(lat2));

                location.setLatitude(Math.toDegrees(lat2));
                location.setLongitude(Math.toDegrees(lon2));
            } else
                location = null;
        }

        return location;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_send:
                Location location = getDistantLocation();
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_LOCATION, location);
                setResult(RESULT_OK, intent);
                finish();
                break;
        }
    }
}
