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

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.dialog.UserDataDialog;
import com.nextgis.safeforest.fragment.CreateMessageFragment;
import com.nextgis.safeforest.fragment.CreateMessageOrientationFragment;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.IMessage;

import java.util.Locale;

import static com.nextgis.maplib.util.Constants.TAG;


public class CreateMessageActivity
        extends SFActivity {
    protected ViewPager mViewPager;
    protected SectionsPagerAdapter mSectionsPagerAdapter;

    protected String mEmailText;
    protected String mContactsText;

    protected int mMessageType = Constants.MSG_TYPE_UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_message);
        setToolbar(R.id.main_toolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        if (tabLayout.getTabCount() < mSectionsPagerAdapter.getCount()) {
            for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                tabLayout.addTab(tabLayout.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)));
            }
        }

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
            case R.id.action_cancel:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void onSave() {
        final UserDataDialog dialog = new UserDataDialog();
        // TODO: set from a app var for the temporary storing
//        dialog.setEmailText(app.getEmailText());
//        dialog.setContactsText(app.getContactsText());
        dialog.setOnPositiveClickedListener(
                new UserDataDialog.OnPositiveClickedListener() {
                    @Override
                    public void onPositiveClicked() {
                        mEmailText = dialog.getEmailText();
                        mContactsText = dialog.getContactsText();

                        if (TextUtils.isEmpty(mEmailText)) {
                            Toast.makeText(CreateMessageActivity.this, R.string.email_hint, Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }

                        if (TextUtils.isEmpty(mContactsText)) {
                            Toast.makeText(CreateMessageActivity.this, R.string.contacts_hint, Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }

                        saveMessage();
                        finish();
                    }
                });
        dialog.setKeepInstance(true);
        dialog.show(getSupportFragmentManager(), Constants.FRAGMENT_USER_DATA_DIALOG);
    }


    protected void saveMessage() {
        try {
            final MainApplication app = (MainApplication) getApplication();

            ContentValues values = mSectionsPagerAdapter.getMessageData(mViewPager.getCurrentItem());
            values.put(Constants.FIELD_MTYPE, mMessageType);
            values.put(Constants.FIELD_STATUS, Constants.MSG_STATUS_NEW);
            values.put(Constants.FIELD_AUTHOR, mEmailText); // TODO authorized user values
            values.put(Constants.FIELD_CONTACT, mContactsText);

            Uri uri = Uri.parse("content://" + app.getAuthority() + "/" + Constants.KEY_CITIZEN_MESSAGES);
            Uri result = app.getContentResolver().insert(uri, values);

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

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            if (position == 0) {
                return new CreateMessageFragment();
            } else {
                return new CreateMessageOrientationFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_current_location).toUpperCase(l);
                case 1:
                    return getString(R.string.title_orientation).toUpperCase(l);
            }
            return null;
        }

        protected Fragment getFragmentByTag(int position) {
            return getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":" + position);
        }

        public ContentValues getMessageData(int position) throws RuntimeException {
            Fragment page = getFragmentByTag(position);

            if (page != null && page instanceof IMessage)
                return ((IMessage) page).getMessageData();
            else
                return new ContentValues();
        }
    }

}
