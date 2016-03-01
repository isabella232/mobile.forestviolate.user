/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
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

import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.ViewMessageActivity;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.UiUtil;

import java.io.IOException;
import java.text.DateFormat;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class ViewMessageFragment
        extends Fragment
{
    protected long   mMessageId;
    protected int    mDataType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle extras = getActivity().getIntent().getExtras();
        if (null != extras) {
            mMessageId = extras.getLong(Constants.FIELD_ID, -1);
            mDataType = extras.getInt(Constants.FIELD_DATA_TYPE);
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        int layout = mDataType == 0 ? R.layout.fragment_view_message : R.layout.fragment_view_document;
        View view = inflater.inflate(layout, container, false);
        MainApplication app = (MainApplication) getActivity().getApplicationContext();
        Uri uri = Uri.parse("content://" + app.getAuthority());
        Cursor cursor = null;
        String[] projection;

        if (mMessageId == -1)
            return view;

        try {
            switch (mDataType) {
                case 0:
                    projection = new String[] {
                            Constants.FIELD_MDATE,
                            Constants.FIELD_AUTHOR,
                            Constants.FIELD_STATUS,
                            Constants.FIELD_MTYPE,
                            Constants.FIELD_MESSAGE,
                            Constants.FIELD_STMESSAGE,
                            FIELD_GEOM};

                    uri = Uri.withAppendedPath(uri, Constants.KEY_CITIZEN_MESSAGES + "/" + mMessageId);
                    cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

                    if (null != cursor && cursor.moveToFirst())
                        fillMessageView(view, cursor);
                    break;
                case 1:
                    projection = new String[] {
                            Constants.FIELD_DOC_ID,
                            Constants.FIELD_DOC_TYPE,
                            Constants.FIELD_DOC_DATE,
                            Constants.FIELD_DOC_USER,
                            Constants.FIELD_DOC_STATUS,
                            Constants.FIELD_DOC_VIOLATE,
                            Constants.FIELD_DOC_PLACE,
                            Constants.FIELD_DOC_DATE_PICK,
                            Constants.FIELD_DOC_DATE_VIOLATE,
                            Constants.FIELD_DOC_FOREST_CAT,
                            Constants.FIELD_DOC_TERRITORY,
                            Constants.FIELD_DOC_REGION};

                    uri = Uri.withAppendedPath(uri, Constants.KEY_FV_DOCS + "/" + mMessageId);
                    cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

                    if (null != cursor && cursor.moveToFirst())
                        fillDocumentView(view, cursor);
                    break;
            }
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            cursor = null;
        }

        if (null != cursor)
            cursor.close();

        return view;
    }

    private void fillMessageView(View view, Cursor cursor) {
        TextView dateView = (TextView) view.findViewById(R.id.date);
        TextView authorView = (TextView) view.findViewById(R.id.author);
        TextView statusView = (TextView) view.findViewById(R.id.status);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        TextView managerMessageView = (TextView) view.findViewById(R.id.stmessage);
        TextView coordinatesView = (TextView) view.findViewById(R.id.coordinates);

        try {
            long timeInMillis = cursor.getLong(cursor.getColumnIndex(Constants.FIELD_MDATE));
            dateView.setText(UiUtil.formatDate(timeInMillis, DateFormat.LONG));

            String data;
            int status = cursor.getInt(cursor.getColumnIndex(Constants.FIELD_STATUS));
            switch (status) {
                case Constants.MSG_STATUS_UNKNOWN:
                default:
                    data = getString(R.string.unknown_message_status);
                    break;
                case Constants.MSG_STATUS_NEW:
                    data = getString(R.string.new_message_status);
                    break;
                case Constants.MSG_STATUS_SENT:
                    data = getString(R.string.sent_message_status);
                    break;
                case Constants.MSG_STATUS_ACCEPTED:
                    data = getString(R.string.accepted_message_status);
                    break;
                case Constants.MSG_STATUS_NOT_ACCEPTED:
                    data = getString(R.string.not_accepted_message_status);
                    break;
                case Constants.MSG_STATUS_CHECKED:
                    data = getString(R.string.checked_message_status);
                    break;
            }
            statusView.setText(data);

            int type = cursor.getInt(cursor.getColumnIndex(Constants.FIELD_MTYPE));
            switch (type) {
                case Constants.MSG_TYPE_UNKNOWN:
                default:
                    data = getString(R.string.unknown_message_type);
                    break;
                case Constants.MSG_TYPE_FIRE:
                    data = getString(R.string.fire);
                    break;
                case Constants.MSG_TYPE_FELLING:
                    data = getString(R.string.action_felling);
                    break;
                case Constants.MSG_TYPE_GARBAGE:
                    data = getString(R.string.garbage);
                    break;
                case Constants.MSG_TYPE_MISC:
                    data = getString(R.string.misc);
                    break;
            }
            ((ViewMessageActivity) getActivity()).setTitle(data);

            GeoMultiPoint mpt = (GeoMultiPoint) GeoGeometryFactory.fromBlob(
                    cursor.getBlob(cursor.getColumnIndex(FIELD_GEOM)));
            GeoPoint pt = mpt.get(0);
            pt.setCRS(CRS_WEB_MERCATOR);
            pt.project(CRS_WGS84);

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            int format = prefs.getInt(
                    SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int",
                    Location.FORMAT_SECONDS);
            int fraction = prefs.getInt(SettingsConstantsUI.KEY_PREF_COORD_FRACTION, 6);

            String lat =
                    getString(com.nextgis.maplibui.R.string.latitude_caption_short) + ": " +
                            LocationUtil.formatLatitude(pt.getY(), format, fraction, getResources());
            String lon =
                    getString(com.nextgis.maplibui.R.string.longitude_caption_short) + ": " +
                            LocationUtil.formatLongitude(pt.getX(), format, fraction, getResources());

            data = lat + "; " + lon;

            coordinatesView.setText(data);
            authorView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_AUTHOR)));
            messageView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_MESSAGE)));
            managerMessageView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_STMESSAGE)));
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void fillDocumentView(View view, Cursor cursor) {
        TextView idView = (TextView) view.findViewById(R.id.id);
        TextView dateView = (TextView) view.findViewById(R.id.date);
        TextView authorView = (TextView) view.findViewById(R.id.author);
        TextView statusView = (TextView) view.findViewById(R.id.status);
        TextView violateView = (TextView) view.findViewById(R.id.violate);
        TextView placeView = (TextView) view.findViewById(R.id.place);
        TextView datePickView = (TextView) view.findViewById(R.id.date_pick);
        TextView dateViolateView = (TextView) view.findViewById(R.id.date_violate);
        TextView forestCategoryView = (TextView) view.findViewById(R.id.forest_category);
        TextView territoryView = (TextView) view.findViewById(R.id.territory);
        TextView regionView = (TextView) view.findViewById(R.id.region);

        long timeInMillis = cursor.getLong(cursor.getColumnIndex(Constants.FIELD_DOC_DATE));
        dateView.setText(UiUtil.formatDate(timeInMillis, DateFormat.LONG));
        idView.setText(String.format("%d", cursor.getLong(cursor.getColumnIndex(Constants.FIELD_DOC_ID))));

        String data;
        int status = cursor.getInt(cursor.getColumnIndex(Constants.FIELD_DOC_STATUS));
        switch (status) {
            case Constants.MSG_STATUS_UNKNOWN:
            default:
                data = getString(R.string.unknown_message_status);
                break;
            case Constants.MSG_STATUS_NEW:
                data = getString(R.string.new_message_status);
                break;
            case Constants.MSG_STATUS_SENT:
                data = getString(R.string.sent_message_status);
                break;
            case Constants.MSG_STATUS_ACCEPTED:
                data = getString(R.string.accepted_message_status);
                break;
            case Constants.MSG_STATUS_NOT_ACCEPTED:
                data = getString(R.string.not_accepted_message_status);
                break;
            case Constants.MSG_STATUS_CHECKED:
                data = getString(R.string.checked_message_status);
                break;
        }
        statusView.setText(data);

        int type = cursor.getInt(cursor.getColumnIndex(Constants.FIELD_DOC_TYPE));
        switch (type) {
            case Constants.DOC_TYPE_INDICTMENT:
                data = getString(R.string.doc_type_indictment);
                break;
            case Constants.DOC_TYPE_FIELD_WORKS:
                data = getString(R.string.doc_type_field_works);
                break;
        }
        ((ViewMessageActivity) getActivity()).setTitle(data);

        authorView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_USER)));
        violateView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_VIOLATE)));
        placeView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_PLACE)));
        timeInMillis = cursor.getLong(cursor.getColumnIndex(Constants.FIELD_DOC_DATE_PICK));
        datePickView.setText(UiUtil.formatDate(timeInMillis, DateFormat.LONG));
        dateViolateView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_DATE_VIOLATE)));
        forestCategoryView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_FOREST_CAT)));
        territoryView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_TERRITORY)));
        regionView.setText(cursor.getString(cursor.getColumnIndex(Constants.FIELD_DOC_REGION)));
    }
}
