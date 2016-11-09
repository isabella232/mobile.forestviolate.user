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

import android.text.TextUtils;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.Layer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.safeforest.util.Constants;

public class MessageFeatureRenderer extends SimpleFeatureRenderer {
    private String mAccount;

    public MessageFeatureRenderer(Layer layer) {
        super(layer);
        mStyle = new MessageMarkerStyle();
    }

    @Override
    protected Style getStyle(long featureId) {
        Feature feature = ((VectorLayer) mLayerRef.get()).getFeature(featureId);
        long type = (long) feature.getFieldValue(Constants.FIELD_MTYPE);
        long status = (long) feature.getFieldValue(Constants.FIELD_STATUS);
        String account = (String) feature.getFieldValue(Constants.FIELD_AUTHOR);
        boolean isOwner = !TextUtils.isEmpty(account) && account.equals(mAccount);
        mStyle.setColor(isOwner ? Constants.COLOR_OWNER : Constants.COLOR_OTHERS);
        ((MessageMarkerStyle) mStyle).setType((int) type);
        ((MessageMarkerStyle) mStyle).setText(status == Constants.MSG_STATUS_DELETED ? "" : null);

        return mStyle;
    }

    public void setAccount(String account) {
        mAccount = account;
    }
}
