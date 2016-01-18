/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
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
    protected TextView mDateView;
    protected TextView mAuthorView;
    protected TextView mStatusView;
    protected TextView mTypeView;
    protected TextView mMessageView;
    protected TextView mManagerMessageView;
    protected TextView mCoordinatesView;

    protected Long   mMessageId;
    protected String mDate;
    protected String mAuthor;
    protected String mStatus;
    protected String mType;
    protected String mMessage;
    protected String mManagerMessage;
    protected String mGeometry;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle extras = getActivity().getIntent().getExtras();
        if (null != extras) {
            mMessageId = extras.getLong(Constants.FIELD_ID);
        }

        if (null == mMessageId) {
            return;
        }

        MainApplication app = (MainApplication) getActivity().getApplicationContext();

        Uri uri = Uri.parse(
                "content://" + app.getAuthority() + "/" + Constants.KEY_CITIZEN_MESSAGES + "/" + mMessageId);

        String[] projection = {
                Constants.FIELD_MDATE,
                Constants.FIELD_AUTHOR,
                Constants.FIELD_STATUS,
                Constants.FIELD_MTYPE,
                Constants.FIELD_MESSAGE,
                Constants.FIELD_STMESSAGE,
                FIELD_GEOM};

        Cursor cursor;
        try {
            cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        } catch (Exception e) {
            Log.d(TAG, e.getLocalizedMessage());
            cursor = null;
        }

        if (null != cursor && cursor.moveToFirst()) {

            try {
                long timeInMillis = cursor.getLong(cursor.getColumnIndex(Constants.FIELD_MDATE));
                mDate = UiUtil.formatDate(timeInMillis, DateFormat.LONG);

                int status = cursor.getInt(cursor.getColumnIndex(Constants.FIELD_STATUS));
                switch (status) {
                    case Constants.MSG_STATUS_UNKNOWN:
                    default:
                        mStatus = getString(R.string.unknown_message_status);
                        break;
                    case Constants.MSG_STATUS_NEW:
                        mStatus = getString(R.string.new_message_status);
                        break;
                    case Constants.MSG_STATUS_SENT:
                        mStatus = getString(R.string.sent_message_status);
                        break;
                    case Constants.MSG_STATUS_ACCEPTED:
                        mStatus = getString(R.string.accepted_message_status);
                        break;
                    case Constants.MSG_STATUS_NOT_ACCEPTED:
                        mStatus = getString(R.string.not_accepted_message_status);
                        break;
                    case Constants.MSG_STATUS_CHECKED:
                        mStatus = getString(R.string.checked_message_status);
                        break;
                }


                int type = cursor.getInt(cursor.getColumnIndex(Constants.FIELD_MTYPE));
                switch (type) {
                    case Constants.MSG_TYPE_UNKNOWN:
                    default:
                        mType = getString(R.string.unknown_message_type);
                        break;
                    case Constants.MSG_TYPE_FIRE:
                        mType = getString(R.string.fire_message_type);
                        break;
                    case Constants.MSG_TYPE_FELLING:
                        mType = getString(R.string.felling_message_type);
                        break;
                }


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

                String lat =
                        getString(com.nextgis.maplibui.R.string.latitude_caption_short) + ": " +
                        LocationUtil.formatLatitude(pt.getY(), format, getResources());
                String lon =
                        getString(com.nextgis.maplibui.R.string.longitude_caption_short) + ": " +
                        LocationUtil.formatLongitude(pt.getX(), format, getResources());

                mGeometry = lat + "; " + lon;


                mAuthor = cursor.getString(cursor.getColumnIndex(Constants.FIELD_AUTHOR));
                mMessage = cursor.getString(cursor.getColumnIndex(Constants.FIELD_MESSAGE));
                mManagerMessage =
                        cursor.getString(cursor.getColumnIndex(Constants.FIELD_STMESSAGE));

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }

            cursor.close();
        }
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_view_message, container, false);

        mDateView = (TextView) view.findViewById(R.id.date);
        mDateView.setText(mDate);

        mAuthorView = (TextView) view.findViewById(R.id.author);
        mAuthorView.setText(mAuthor);

        mStatusView = (TextView) view.findViewById(R.id.status);
        mStatusView.setText(mStatus);

        mTypeView = (TextView) view.findViewById(R.id.type);
        mTypeView.setText(mType);

        mMessageView = (TextView) view.findViewById(R.id.message);
        mMessageView.setText(mMessage);

        mManagerMessageView = (TextView) view.findViewById(R.id.stmessage);
        mManagerMessageView.setText(mManagerMessage);

        mCoordinatesView = (TextView) view.findViewById(R.id.coordinates);
        mCoordinatesView.setText(mGeometry);

        return view;
    }
}
