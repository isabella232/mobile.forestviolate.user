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

package com.nextgis.safeforest.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;

import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapEventSource;
import com.nextgis.safeforest.util.Constants;

public class MessagesLoader extends AsyncTaskLoader<Cursor> {
    private MapEventSource mMap;

    public MessagesLoader(Context context) {
        super(context);
        mMap = (MapEventSource) MapBase.getInstance();
    }

    @Override
    public Cursor loadInBackground() {
        if (null == mMap)
            return null;

        SQLiteDatabase db = mMap.getDatabase(true);

        String query = String.format("select %s, %s, %s, %s, %s, %s, %s, 0 as %s from %s union all " +
                        "select %s, %s as %s, %s as %s, %s as %s, %s as %s, %s as %s, %s, 1 as %s from %s where %s IN (%d, %d) order by %s desc",
                Constants.FIELD_ID, Constants.FIELD_MDATE, Constants.FIELD_AUTHOR, Constants.FIELD_STATUS, Constants.FIELD_MTYPE, Constants.FIELD_MESSAGE,
                com.nextgis.maplib.util.Constants.FIELD_GEOM, Constants.FIELD_DATA_TYPE, Constants.KEY_CITIZEN_MESSAGES,
                Constants.FIELD_ID, Constants.FIELD_DOC_DATE, Constants.FIELD_MDATE, Constants.FIELD_DOC_USER, Constants.FIELD_AUTHOR,
                Constants.FIELD_DOC_STATUS, Constants.FIELD_STATUS, Constants.FIELD_DOC_TYPE, Constants.FIELD_MTYPE,
                Constants.FIELD_DOC_VIOLATE, Constants.FIELD_MESSAGE, com.nextgis.maplib.util.Constants.FIELD_GEOM,
                Constants.FIELD_DATA_TYPE, Constants.KEY_FV_DOCS, Constants.FIELD_DOC_TYPE, Constants.DOC_TYPE_FIELD_WORKS, Constants.DOC_TYPE_INDICTMENT,
                Constants.FIELD_MDATE);

        return db.rawQuery(query, null);
    }
}