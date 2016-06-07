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

package com.nextgis.safeforest.mapui;

import android.content.Context;
import android.content.SyncResult;
import android.database.sqlite.SQLiteException;

import com.nextgis.maplib.util.AccountUtil;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MessageLayerUI
        extends NGWVectorLayerUI {
    protected static final String JSON_INSERT_ID_KEY = "insert_id";


    protected long mQueryRemoteId;


    public MessageLayerUI(Context context,File path) {
        super(context, path);
    }


    public long getQueryRemoteId() {
        return mQueryRemoteId;
    }


    public void setQueryRemoteId(long queryRemoteId) {
        mQueryRemoteId = queryRemoteId;
    }


    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException, SQLiteException {
        super.fromJSON(jsonObject);

        if (jsonObject.has(JSON_INSERT_ID_KEY)) {
            mQueryRemoteId = jsonObject.getLong(JSON_INSERT_ID_KEY);
        }
    }


    @Override
    public JSONObject toJSON()
            throws JSONException {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_INSERT_ID_KEY, mQueryRemoteId);

        return rootConfig;
    }


    @Override
    protected String getFeaturesUrl(AccountUtil.AccountData accountData) {
        return NGWUtil.getFeaturesUrl(accountData.url, mQueryRemoteId, mServerWhere);
    }


    @Override
    protected String getResourceMetaUrl(AccountUtil.AccountData accountData) {
        return NGWUtil.getResourceMetaUrl(accountData.url, mQueryRemoteId);
    }


    @Override
    protected String getRequiredCls() {
        return "query_layer";
    }


    @Override
    protected boolean sendAttachOnServer(
            long featureId,
            long attachId,
            SyncResult syncResult) {
        throw new UnsupportedOperationException("NotesLayerUI do not support this operation.");
    }
}
