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

package com.nextgis.safeforest.util;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FeatureChanges;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.display.MessageFeatureRenderer;

import java.util.Date;
import java.util.Map;

public final class MapUtil {
    public static boolean hasLayer(MapBase map, String layer) {
        return map != null && map.getLayerByName(layer) != null;
    }

    public static boolean isRegionSet(SharedPreferences preferences) {
        return preferences.contains(SettingsConstants.KEY_PREF_USERMINX) && preferences.contains(SettingsConstants.KEY_PREF_USERMAXX)
                && preferences.contains(SettingsConstants.KEY_PREF_USERMINY) && preferences.contains(SettingsConstants.KEY_PREF_USERMAXY);
    }

    public static boolean checkServerLayers(INGWResource resource, Map<String, Resource> keys) {
        if (resource instanceof Connection) {
            Connection connection = (Connection) resource;
            connection.loadChildren();
        } else if (resource instanceof ResourceGroup) {
            ResourceGroup resourceGroup = (ResourceGroup) resource;
            resourceGroup.loadChildren();
        }

        for (int i = 0; i < resource.getChildrenCount(); ++i) {
            INGWResource childResource = resource.getChild(i);

            if (keys.containsKey(childResource.getKey()) && childResource instanceof Resource) {
                Resource ngwResource = (Resource) childResource;
                keys.put(ngwResource.getKey(), ngwResource);
            }

            boolean bIsFill = true;
            for (Map.Entry<String, Resource> entry : keys.entrySet()) {
                if (entry.getValue() == null) {
                    bIsFill = false;
                    break;
                }
            }

            if (bIsFill) {
                return true;
            }

            if (checkServerLayers(childResource, keys)) {
                return true;
            }
        }

        boolean bIsFill = true;

        for (Map.Entry<String, Resource> entry : keys.entrySet()) {
            if (entry.getValue() == null) {
                bIsFill = false;
                break;
            }
        }

        return bIsFill;
    }

    public static void removeOutdatedChanges(NGWVectorLayer layer) {
        if (layer == null)
            return;

        String changeTableName = layer.getPath().getName() + com.nextgis.maplib.util.Constants.CHANGES_NAME_POSTFIX;

        try {
            Cursor changeCursor = FeatureChanges.getChanges(changeTableName);

            if (changeCursor == null)
                return;

            if (!changeCursor.moveToFirst()) {
                changeCursor.close();
                return;
            }

            int recordIdColumn = changeCursor.getColumnIndex(com.nextgis.maplib.util.Constants.FIELD_ID);
            int featureIdColumn = changeCursor.getColumnIndex(com.nextgis.maplib.util.Constants.FIELD_FEATURE_ID);
            long changeRecordId, changeFeatureId;

            do {
                changeRecordId = changeCursor.getLong(recordIdColumn);
                changeFeatureId = changeCursor.getLong(featureIdColumn);

                Feature feature = layer.getFeature(changeFeatureId);
                if (feature != null) {
                    int type = ((Long) feature.getFieldValue(Constants.FIELD_MTYPE)).intValue();

                    long mdate = 0;
                    Object field = feature.getFieldValue(Constants.FIELD_MDATE);
                    if (field instanceof Date)
                        mdate = ((Date) field).getTime();
                    if (field instanceof Long)
                        mdate = (Long) field;

                    long diff = System.currentTimeMillis() - mdate;
                    boolean outdated = false;
                    switch (type) {
                        case Constants.MSG_TYPE_FIRE:
                            outdated = diff >= Constants.MAX_DIFF_FIRE;
                            break;
                        case Constants.MSG_TYPE_FELLING:
                            outdated = diff >= Constants.MAX_DIFF_FELLING;
                            break;
                        case Constants.MSG_TYPE_GARBAGE:
                        case Constants.MSG_TYPE_MISC:
                            outdated = diff >= Constants.MAX_DIFF_OTHER;
                            break;
                    }

                    if (outdated)
                        FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
                } else
                    FeatureChanges.removeChangeRecord(changeTableName, changeRecordId);
            } while (changeCursor.moveToNext());

            changeCursor.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public static void setMessageRenderer(MapBase map, MainApplication app) {
        VectorLayer messages = ((VectorLayer) map.getLayerByName(Constants.KEY_CITIZEN_MESSAGES));
        if (messages != null) {
            MessageFeatureRenderer renderer = new MessageFeatureRenderer(messages);

            Account account = app.getAccount(app.getString(R.string.account_name));

            if (account != null) {
                String email = app.getAccountUserData(account, com.nextgis.safeforest.util.SettingsConstants.KEY_USER_EMAIL);
                renderer.setAccount(email);
            }

            messages.setRenderer(renderer);
        }
    }
    
    public static String getStatus(Context context, int type) {
        switch (type) {
            case Constants.MSG_STATUS_UNKNOWN:
            default:
                return context.getString(R.string.unknown_message_status);
            case Constants.MSG_STATUS_NEW:
                return context.getString(R.string.new_message_status);
            case Constants.MSG_STATUS_SENT:
                return context.getString(R.string.sent_message_status);
            case Constants.MSG_STATUS_ACCEPTED:
                return context.getString(R.string.accepted_message_status);
            case Constants.MSG_STATUS_NOT_ACCEPTED:
                return context.getString(R.string.not_accepted_message_status);
            case Constants.MSG_STATUS_CHECKED:
                return context.getString(R.string.checked_message_status);
        }
    }
}
