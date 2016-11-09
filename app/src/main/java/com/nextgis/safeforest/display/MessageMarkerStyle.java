/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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

package com.nextgis.safeforest.display;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.safeforest.util.Constants;

public class MessageMarkerStyle extends SimpleMarkerStyle {
    public MessageMarkerStyle() {
        super();
        mWidth = 1;
        mSize = 8;
        mColor = Constants.COLOR_OTHERS;
        mOutColor = Constants.COLOR_OUTLINE;
        setPaintsColors();
    }

    protected void onDraw(GeoPoint pt, GISDisplay display) {
        if (null == pt || mText != null)
            return;

        float scaledSize = (float) (mSize / display.getScale());
        float width = (float) (mWidth / display.getScale());
        switch (mType) {
            case Constants.MSG_TYPE_FELLING:
                drawCircleMarker(scaledSize, width, pt, display);
                break;

            case Constants.MSG_TYPE_FIRE:
                drawDiamondMarker(scaledSize, width, pt, display);
                break;

            case Constants.MSG_TYPE_GARBAGE:
                drawTriangleMarker(scaledSize, width, pt, display);
                break;

            case Constants.MSG_TYPE_MISC:
                drawBoxMarker(scaledSize, width, pt, display);
                break;
        }
    }
}
