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

package com.nextgis.safeforest.activity;

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.keenfin.easypicker.PhotoPicker;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.AccurateLocationTaker;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.dialog.UserDataDialog;
import com.nextgis.safeforest.dialog.YesNoDialog;
import com.nextgis.safeforest.fragment.MapFragment;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.SettingsConstants;
import com.nextgis.safeforest.util.UiUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.nextgis.maplib.util.Constants.FIELD_GEOM;
import static com.nextgis.maplib.util.Constants.TAG;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;


public class CreateMessageActivity
        extends SFActivity implements View.OnClickListener {
    protected static final int MESSAGE_COMPASS = 1;

    protected MainApplication mApp;
    protected ContentValues mValues;
    protected String mEmailText, mPhoneText, mFullNameText;

    protected int mMessageType = Constants.MSG_TYPE_UNKNOWN;
    protected EditText mMessage;
    protected FloatingActionButton mSend, mLocationCurrent, mAddPhoto, mLocationCompass;
    protected MapFragment mMapFragment;
    protected ActionBar mToolbar;
    protected FrameLayout mPhotos;
    protected int mTitle;
    protected MenuItem mItem;
    protected PhotoPicker mPhotoPicker;
    protected UserDataDialog mUserDataDialog;
    protected YesNoDialog mAuthDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
        setToolbar(R.id.main_toolbar);

        mValues = new ContentValues();
        mApp = (MainApplication) getApplication();
        mToolbar = getSupportActionBar();

        Bundle extras = getIntent().getExtras();
        if (null != extras) {
            mMessageType = extras.getInt(Constants.FIELD_MTYPE);
            mTitle = R.string.new_message_status;

            switch (mMessageType) {
                case Constants.MSG_TYPE_FELLING:
                    mTitle = R.string.action_felling;
                    break;
                case Constants.MSG_TYPE_FIRE:
                    mTitle = R.string.fire;
                    break;
                case Constants.MSG_TYPE_GARBAGE:
                    mTitle = R.string.garbage;
                    break;
                case Constants.MSG_TYPE_MISC:
                    mTitle = R.string.misc;
                    break;
            }

            mToolbar.setTitle(mTitle);
        }

        mPhotoPicker = (PhotoPicker) findViewById(R.id.pp_violation);
        mPhotos = (FrameLayout) findViewById(R.id.fl_photos);
        mMessage = (EditText) findViewById(R.id.message);

        mSend = (FloatingActionButton) findViewById(R.id.action_send);
        mSend.setOnClickListener(this);

        mAddPhoto = (FloatingActionButton) findViewById(R.id.action_photo);
        mAddPhoto.setOnClickListener(this);

        mLocationCurrent = (FloatingActionButton) findViewById(R.id.action_location);
        mLocationCurrent.setOnClickListener(this);

        mLocationCompass = (FloatingActionButton) findViewById(R.id.action_compass);
        mLocationCompass.setOnClickListener(this);

        mUserDataDialog = (UserDataDialog) getSupportFragmentManager().findFragmentByTag(Constants.FRAGMENT_USER_DATA_DIALOG);
        mAuthDialog = (YesNoDialog) getSupportFragmentManager().findFragmentByTag(Constants.FRAGMENT_USER_AUTH);
    }

    private void addMap() {
        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        mMapFragment = (MapFragment) fm.findFragmentByTag(Constants.FRAGMENT_SELECT_LOCATION);
        if (mMapFragment == null)
            mMapFragment = new MapFragment();

        ft.replace(R.id.container, mMapFragment, Constants.FRAGMENT_SELECT_LOCATION).commit();
        mMapFragment.setSelectedLocationVisible(true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Account account = mApp.getAccount(getString(R.string.account_name));
        String auth = mApp.getAccountUserData(account, Constants.KEY_IS_AUTHORIZED);
        boolean isAuthorized = auth != null && !auth.equals(Constants.ANONYMOUS);
        boolean isUserDataValid = hasPhoneAndName();
        mPhoneText = mApp.getAccountUserData(account, SettingsConstants.KEY_USER_PHONE);
        mFullNameText = mApp.getAccountUserData(account, SettingsConstants.KEY_USER_FULLNAME);
        mEmailText = mApp.getAccountUserData(account, SettingsConstants.KEY_USER_EMAIL);

        if (isAuthorized)
            mEmailText = mApp.getAccountLogin(account);

        if (isAuthorized || isUserDataValid)
            addMap();
        else
            showUserDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mMapFragment != null)
            mMapFragment.addMap();
    }

    private boolean hasPhoneAndName() {
        return UiUtil.isPhoneValid(mPhoneText) && !TextUtils.isEmpty(mFullNameText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message, menu);
        mItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.action_center:
                mMapFragment.centerSelectedPoint();
                break;
            case R.id.action_locate:
                if (mPhotos.getVisibility() == View.GONE)
                    mMapFragment.locateCurrentPosition();
                else
                    hidePhotos();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void showUserDialog() {
        if (mUserDataDialog == null) {
            mUserDataDialog = new UserDataDialog();
            mUserDataDialog.setCancelable(false);
            mUserDataDialog.setKeepInstance(true);
            mUserDataDialog.setFullNameText(mFullNameText);
            mUserDataDialog.setPhoneText(mPhoneText);
            mUserDataDialog.setEmailText(mEmailText);
        }

        mUserDataDialog.setOnPositiveClickedListener(new YesNoDialog.OnPositiveClickedListener() {
            @Override
            public void onPositiveClicked() {
                mFullNameText = mUserDataDialog.getFullNameText();
                mPhoneText = mUserDataDialog.getPhoneText();
                mEmailText = mUserDataDialog.getEmailText();

                if (TextUtils.isEmpty(mFullNameText) || TextUtils.isEmpty(mPhoneText)) {
                    Toast.makeText(CreateMessageActivity.this, R.string.anonymous_hint, Toast.LENGTH_LONG).show();
                    return;
                }

                if (!UiUtil.isPhoneValid(mPhoneText)) {
                    Toast.makeText(CreateMessageActivity.this, R.string.phone_not_valid, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(mEmailText)) {
                    if (!UiUtil.isEmailValid(mEmailText)) {
                        Toast.makeText(CreateMessageActivity.this, R.string.email_not_valid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                String accountName = getString(R.string.account_name);
                mApp.setUserData(accountName, SettingsConstants.KEY_USER_FULLNAME, mFullNameText);
                mApp.setUserData(accountName, SettingsConstants.KEY_USER_PHONE, mPhoneText);
                mApp.setUserData(accountName, SettingsConstants.KEY_USER_EMAIL, mEmailText);

                mUserDataDialog.dismiss();
                addMap();
            }
        });
        mUserDataDialog.setOnNegativeClickedListener(new YesNoDialog.OnNegativeClickedListener() {
            @Override
            public void onNegativeClicked() {
                showSignUpDialog();
                mUserDataDialog.dismiss();
            }
        });

        if (!mUserDataDialog.isAdded())
            mUserDataDialog.show(getSupportFragmentManager(), Constants.FRAGMENT_USER_DATA_DIALOG);
    }

    protected void showSignUpDialog() {
        if (mAuthDialog == null) {
            mAuthDialog = new YesNoDialog();
            mAuthDialog.setCancelable(false);
            mAuthDialog.setKeepInstance(true);
            mAuthDialog.setTitle(R.string.auth_title)
                    .setMessage(R.string.auth_require)
                    .setPositiveText(android.R.string.yes)
                    .setNegativeText(android.R.string.no);
        }

        mAuthDialog.setOnPositiveClickedListener(new YesNoDialog.OnPositiveClickedListener() {
            @Override
            public void onPositiveClicked() {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                boolean isConnected = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();

                if (isConnected) {
                    Intent accountSettings = new Intent(CreateMessageActivity.this, AccountActivity.class);
                    accountSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    accountSettings.setAction(Constants.FRAGMENT_LOGIN);
                    startActivity(accountSettings);
                } else
                    Toast.makeText(CreateMessageActivity.this, R.string.error_network_unavailable, Toast.LENGTH_SHORT).show();

                mAuthDialog.dismiss();
                addMap();
            }
        });
        mAuthDialog.setOnNegativeClickedListener(new YesNoDialog.OnNegativeClickedListener() {
            @Override
            public void onNegativeClicked() {
                mAuthDialog.dismiss();
                finish();
            }
        });

        if (!mAuthDialog.isAdded())
            mAuthDialog.show(getSupportFragmentManager(), Constants.FRAGMENT_USER_AUTH);
    }

    protected void sendMessage() {
        mSend.setEnabled(false);
        saveLocation(mMapFragment.getSelectedLocation());

        try {
            mValues.put(Constants.FIELD_MTYPE, mMessageType);
            mValues.put(Constants.FIELD_STATUS, Constants.MSG_STATUS_NEW);
            mValues.put(Constants.FIELD_MESSAGE, mMessage.getText().toString());
            mValues.put(Constants.FIELD_AUTHOR, mEmailText);
            mValues.put(Constants.FIELD_CONTACT, mPhoneText + ", " + mFullNameText);

            Uri uri = Uri.parse("content://" + mApp.getAuthority() + "/" + Constants.KEY_CITIZEN_MESSAGES);
            Uri result = mApp.getContentResolver().insert(uri, mValues);

            if (result == null) {
                Log.d(TAG, "MessageFragment, saveMessage(), Layer: " + Constants.KEY_CITIZEN_MESSAGES + ", insert FAILED");
                Toast.makeText(mApp, R.string.error_create_message, Toast.LENGTH_LONG).show();
            } else {
                long id = Long.parseLong(result.getLastPathSegment());
                Log.d(TAG, "MessageFragment, saveMessage(), Layer: " + Constants.KEY_CITIZEN_MESSAGES
                        + ", id: " + id + ", insert result: " + result);

                putAttaches(Uri.parse("content://" + mApp.getAuthority() + "/" +
                        Constants.KEY_CITIZEN_MESSAGES + "/" + id + "/attach"));

                Toast.makeText(mApp, R.string.message_created, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (RuntimeException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

        mSend.setEnabled(true);
    }

    private void putAttaches(Uri uri) {
        for (String path : mPhotoPicker.getImagesPath()) {
            String[] segments = path.split("/");
            String name = segments.length > 0 ? segments[segments.length - 1] : "image.jpg";
            ContentValues values = new ContentValues();
            values.put(VectorLayer.ATTACH_DISPLAY_NAME, name);
            values.put(VectorLayer.ATTACH_MIME_TYPE, "image/jpeg");

            Uri result = getContentResolver().insert(uri, values);
            if (result == null) {
                Log.d(TAG, "attach insert failed");
            } else {
                try {
                    OutputStream outStream = getContentResolver().openOutputStream(result);

                    if (outStream != null) {
                        InputStream inStream = new FileInputStream(path);
                        byte[] buffer = new byte[8192];
                        int counter;

                        while ((counter = inStream.read(buffer, 0, buffer.length)) > 0) {
                            outStream.write(buffer, 0, counter);
                            outStream.flush();
                        }

                        outStream.close();
                        inStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "attach insert success: " + result.toString());
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action_send:
                sendMessage();
                break;
            case R.id.action_location:
                getCurrentLocation();
                break;
            case R.id.action_photo:
                showPhotos();
                break;
            case R.id.action_compass:
                Intent compassIntent = new Intent(this, MessageCompassActivity.class);
                startActivityForResult(compassIntent, MESSAGE_COMPASS);
                break;
        }
    }

    private void showPhotos() {
        mPhotos.setVisibility(View.VISIBLE);
        mToolbar.setDisplayHomeAsUpEnabled(false);
        mToolbar.setTitle(R.string.photo_add);
        mItem.setIcon(R.drawable.ic_action_apply_dark);
    }

    private void hidePhotos() {
        mPhotos.setVisibility(View.GONE);
        mToolbar.setDisplayHomeAsUpEnabled(true);
        mToolbar.setTitle(mTitle);
        mItem.setIcon(R.drawable.ic_my_location_white_24dp);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MESSAGE_COMPASS:
                if (data != null)
                    mMapFragment.setSelectedLocation((Location) data.getParcelableExtra(Constants.KEY_LOCATION));
                break;
            default:
                mPhotoPicker.onActivityResult(requestCode, resultCode, data);
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    protected void getCurrentLocation() {
        final ProgressDialog progress;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            progress = new ProgressDialog(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        else
            progress = new ProgressDialog(this);

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
                mMapFragment.setSelectedLocation(accurateLocation);
                progress.dismiss();
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
