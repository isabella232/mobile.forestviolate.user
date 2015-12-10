/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
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

package com.nextgis.safeforest.activity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.AccurateLocationTaker;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.dialog.UserDataDialog;
import com.nextgis.safeforest.dialog.YesNoDialog;
import com.nextgis.safeforest.util.Constants;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class CreateMessageActivity
        extends SFActivity implements View.OnClickListener {
    protected static final int MESSAGE_MAP = 0;
    protected static final int MESSAGE_COMPASS = 1;

    protected ContentValues mValues;
    protected String mEmailText, mPhoneText, mFullNameText;

    protected int mMessageType = Constants.MSG_TYPE_UNKNOWN;
    protected EditText mMessage;
    protected TextView mLocationCurrent, mLocationMap, mLocationCompass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mValues = new ContentValues(); // TODO save / restore state
        setContentView(R.layout.activity_create_message);
        setToolbar(R.id.main_toolbar);

        Bundle extras = getIntent().getExtras();
        if (null != extras) {
            mMessageType = extras.getInt(Constants.FIELD_MTYPE);
            int textResId = R.string.new_message_status;

            switch (mMessageType) {
                case Constants.MSG_TYPE_FELLING:
                    textResId = R.string.action_felling;
                    break;
                case Constants.MSG_TYPE_FIRE:
                    textResId = R.string.fire;
                    break;
            }

            //noinspection ConstantConditions
            getSupportActionBar().setTitle(textResId);
        }

        mMessage = (EditText) findViewById(R.id.message);

        mLocationCurrent = (TextView) findViewById(R.id.location_current);
        mLocationCurrent.setOnClickListener(this);
        Drawable drawable = getResources().getDrawable(R.drawable.ic_place_black_48dp);
        tintIcon(drawable);
        mLocationCurrent.setCompoundDrawables(null, drawable, null, null);

        mLocationMap = (TextView) findViewById(R.id.location_map);
        mLocationMap.setOnClickListener(this);
        drawable = getResources().getDrawable(R.drawable.ic_map_black_48dp);
        tintIcon(drawable);
        mLocationMap.setCompoundDrawables(null, drawable, null, null);

        mLocationCompass = (TextView) findViewById(R.id.location_compass);
        mLocationCompass.setOnClickListener(this);
        drawable = getResources().getDrawable(R.drawable.ic_compass);
        tintIcon(drawable);
        mLocationCompass.setCompoundDrawables(null, drawable, null, null);
    }


    protected void tintIcon(Drawable drawable) {
        DrawableCompat.wrap(drawable);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        DrawableCompat.setTint(drawable, getResources().getColor(R.color.accent));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.action_save:
                onSave();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void onSave() {
        final UserDataDialog dialog = new UserDataDialog();
        // TODO: set from a app var for the temporary storing
//        dialog.setEmailText(app.getEmailText());
//        dialog.setFullNameText(app.getFullNameText());
        dialog.setOnPositiveClickedListener(new YesNoDialog.OnPositiveClickedListener() {
            @Override
            public void onPositiveClicked() {
                mFullNameText = dialog.getFullNameText();
                mPhoneText = dialog.getPhoneText();
                mEmailText = dialog.getEmailText();

                if (TextUtils.isEmpty(mFullNameText) || TextUtils.isEmpty(mPhoneText)) {
                    Toast.makeText(CreateMessageActivity.this, R.string.anonymous_hint, Toast.LENGTH_LONG).show();
                    return;
                }

                Pattern pattern = Pattern.compile(Constants.PHONE_PATTERN);
                Matcher matcher = pattern.matcher(mPhoneText);
                if (!matcher.matches()) {
                    Toast.makeText(CreateMessageActivity.this, R.string.phone_not_valid, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(mEmailText)) {
                    pattern = Pattern.compile(Constants.EMAIL_PATTERN);
                    matcher = pattern.matcher(mEmailText);
                    if (!matcher.matches()) {
                        Toast.makeText(CreateMessageActivity.this, R.string.email_not_valid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                saveMessage();
                finish();
            }
        });
        dialog.setOnNegativeClickedListener(new YesNoDialog.OnNegativeClickedListener() {
            @Override
            public void onNegativeClicked() {
                dialog.dismiss();
            }
        });
        dialog.setKeepInstance(true);
        dialog.show(getSupportFragmentManager(), Constants.FRAGMENT_USER_DATA_DIALOG);
    }


    protected void saveMessage() {
        try {
            final MainApplication app = (MainApplication) getApplication();

            mValues.put(Constants.FIELD_MTYPE, mMessageType);
            mValues.put(Constants.FIELD_STATUS, Constants.MSG_STATUS_NEW);
            mValues.put(Constants.FIELD_AUTHOR, mFullNameText); // TODO authorized user values
            mValues.put(Constants.FIELD_CONTACT, mPhoneText + ", " + mEmailText);

            Uri uri = Uri.parse("content://" + app.getAuthority() + "/" + Constants.KEY_CITIZEN_MESSAGES);
            Uri result = app.getContentResolver().insert(uri, mValues);

            if (result == null) {
                Log.d(TAG, "MessageFragment, saveMessage(), Layer: " + Constants.KEY_CITIZEN_MESSAGES + ", insert FAILED");
                Toast.makeText(app, R.string.error_create_message, Toast.LENGTH_LONG).show();
                // TODO: not close activity
            } else {
                long id = Long.parseLong(result.getLastPathSegment());
                Log.d(TAG, "MessageFragment, saveMessage(), Layer: " + Constants.KEY_CITIZEN_MESSAGES
                        + ", id: " + id + ", insert result: " + result);
            }
        } catch (RuntimeException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.location_current:
                getCurrentLocation();
                break;
            case R.id.location_map:
                Intent mapIntent = new Intent(this, MessageMapActivity.class);
                startActivityForResult(mapIntent, MESSAGE_MAP);
                break;
            case R.id.location_compass:
                Intent compassIntent = new Intent(this, MessageCompassActivity.class);
                startActivityForResult(compassIntent, MESSAGE_COMPASS);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MESSAGE_MAP:
            case MESSAGE_COMPASS:
                if (data != null)
                    saveLocation((Location) data.getParcelableExtra(Constants.KEY_LOCATION));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    protected void getCurrentLocation() {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        progress.setCanceledOnTouchOutside(false);
        progress.setMessage(getString(R.string.location_getting_current));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        final AccurateLocationTaker locationTaker = new AccurateLocationTaker(this,
                Constants.MAX_LOCATION_ACCURACY, Constants.MAX_LOCATION_MEASURES,
                Constants.MAX_LOCATION_TIME, Constants.MAX_LOCATION_TIME, null);
        locationTaker.setOnGetAccurateLocationListener(new AccurateLocationTaker.OnGetAccurateLocationListener() {
            @Override
            public void onGetAccurateLocation(Location accurateLocation, Long... values) {
                progress.dismiss();
                saveLocation(accurateLocation);
            }
        });

        progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                locationTaker.cancelTaking();
            }
        });

        progress.show();
        locationTaker.startTaking();
    }

    protected void saveLocation(Location location) {
        if (location == null) {
            Toast.makeText(CreateMessageActivity.this, R.string.error_no_location, Toast.LENGTH_LONG).show();
            return;
        }

        mValues.put(Constants.FIELD_MDATE, location.getTime());

        try {
            GeoPoint pt;
            if (com.nextgis.maplib.util.Constants.DEBUG_MODE) {
                pt = new GeoPoint(0, 0);
            } else {
                pt = new GeoPoint(location.getLongitude(), location.getLatitude());
            }
            pt.setCRS(CRS_WGS84);
            pt.project(CRS_WEB_MERCATOR);
            GeoMultiPoint mpt = new GeoMultiPoint();
            mpt.add(pt);
            mValues.put(FIELD_GEOM, mpt.toBlob());
            Log.d(TAG, "MessageActivity, saveMessage(), point: " + pt.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
