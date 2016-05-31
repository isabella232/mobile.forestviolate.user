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

import android.Manifest;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.espian.showcaseview.ShowcaseView;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.safeforest.BuildConfig;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.fragment.LoginFragment;
import com.nextgis.safeforest.fragment.MapFragment;
import com.nextgis.safeforest.fragment.MessageListFragment;
import com.nextgis.safeforest.fragment.RegionSyncFragment;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.MapUtil;
import com.nextgis.safeforest.util.SettingsConstants;
import com.nextgis.safeforest.util.UiUtil;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.Locale;

public class MainActivity extends SFActivity implements NGWLoginFragment.OnAddAccountListener, View.OnClickListener {
    enum CURRENT_VIEW {ACCOUNT, INITIAL, NORMAL}
    protected static final int PERMISSIONS_REQUEST = 1;
    protected static final String KEY_CURRENT_VIEW = "current_view";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    protected SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    protected ViewPager mViewPager;
    protected TabLayout mTabLayout;
    protected boolean mFirstRun = true;
    protected int mCurrentView;
    protected int mCurrentViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_no_permissions);
        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.app_name));

        mCurrentViewState = savedInstanceState != null ? savedInstanceState.getInt(KEY_CURRENT_VIEW) : -1;
        findViewById(R.id.grant_permissions).setOnClickListener(this);

        if (!hasPermissions())
            requestPermissions();
        else
            start();
    }

    protected void requestPermissions() {
        String[] permissions = new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(R.string.permissions, R.string.requested_permissions, PERMISSIONS_REQUEST, permissions);
    }

    protected boolean hasPermissions() {
        return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                isPermissionGranted(Manifest.permission.GET_ACCOUNTS) &&
                isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST:
                if (isGrantedResult(grantResults))
                    start();
                else
                    Toast.makeText(this, R.string.permissions_deny, Toast.LENGTH_SHORT).show();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void start() {
        // check if first run
        // get from properties if first time
        // and registered or guest user
        final MainApplication app = (MainApplication) getApplication();
        final Account account = app.getAccount(getString(R.string.account_name));
        if (account == null || mCurrentViewState == CURRENT_VIEW.ACCOUNT.ordinal()) {
            Log.d(Constants.SFTAG, "No account. " + getString(R.string.account_name) + " created. Run first step.");
            createFirstStartView();
        } else {
            if (!hasBasicLayers(app.getMap()) || mCurrentViewState == CURRENT_VIEW.INITIAL.ordinal()) {
                Log.d(Constants.SFTAG, "Account " + getString(R.string.account_name) + " created. Run second step.");
                createSecondStartView();
            } else {
                Log.d(Constants.SFTAG, "Account " + getString(R.string.account_name) + " created. Layers created. Run normal view.");
                mFirstRun = false;
                createNormalView();
            }
        }
    }

    private boolean isGrantedResult(int[] grantResults) {
        return grantResults.length == 4 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
                && grantResults[3] == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean hasBasicLayers(MapBase map) {
        return MapUtil.hasLayer(map, Constants.KEY_FV_REGIONS) && MapUtil.hasLayer(map, Constants.KEY_CITIZEN_MESSAGES);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_VIEW, mCurrentView);
    }

    protected void createFirstStartView(){
        mCurrentView = CURRENT_VIEW.ACCOUNT.ordinal();
        setContentView(R.layout.activity_main_first);
        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.first_run));

        FragmentManager fm = getSupportFragmentManager();
        NGWLoginFragment ngwLoginFragment = (NGWLoginFragment) fm.findFragmentByTag(Constants.FRAGMENT_LOGIN);

        if (ngwLoginFragment == null) {
            ngwLoginFragment = new LoginFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(com.nextgis.maplibui.R.id.login_frame, ngwLoginFragment, Constants.FRAGMENT_LOGIN);
            ft.commitAllowingStateLoss();
        }
        ngwLoginFragment.setForNewAccount(true);
        ngwLoginFragment.setOnAddAccountListener(this);
    }

    protected void createSecondStartView(){
        mCurrentView = CURRENT_VIEW.INITIAL.ordinal();
        setContentView(R.layout.activity_main_first);
        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.initialization));

        FragmentManager fm = getSupportFragmentManager();
        RegionSyncFragment initialSyncFragment = (RegionSyncFragment) fm.findFragmentByTag(Constants.FRAGMENT_SYNC_REGION);

        if (initialSyncFragment == null)
            initialSyncFragment = new RegionSyncFragment();

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(com.nextgis.maplibui.R.id.login_frame, initialSyncFragment, Constants.FRAGMENT_SYNC_REGION);
        ft.commit();
    }


    protected void createNormalView() {
        mCurrentView = CURRENT_VIEW.NORMAL.ordinal();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_main);
        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.app_name));

        final ShowcaseView sv = (ShowcaseView) findViewById(R.id.showcase);
        sv.setShotType(ShowcaseView.TYPE_ONE_SHOT);
        sv.setShowcaseView(findViewById(R.id.showcase_template));
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.overrideButtonClick(MainActivity.this);
            }
        });

        FloatingActionsMenu fab = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        fab.overrideAddButton(this);

        // Create the adapter that will return a fragment for each of the primary sections of the
        // activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);

                if (tab.getPosition() == 0) {
                    ((MapFragment) mSectionsPagerAdapter.getItem(1)).pauseGps();
                    setMarginsToFAB(false);
                } else {
                    ((MapFragment) mSectionsPagerAdapter.getItem(1)).resumeGps();
                    animateFAB(0, 1, false);
                    setMarginsToFAB(true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                super.onTabUnselected(tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                super.onTabReselected(tab);
            }
        });

        if(mTabLayout.getTabCount() < mSectionsPagerAdapter.getCount()) {
            // For each of the sections in the app, add a tab to the action bar.
            for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                // Create a tab with text corresponding to the page title defined by
                // the adapter. Also specify this Activity object, which implements
                // the TabListener interface, as the callback (listener) for when
                // this tab is selected.
                mTabLayout.addTab(mTabLayout.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)));
            }
        }

        mViewPager.post(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(1);
            }
        });

        findViewById(R.id.call).setOnClickListener(this);
        findViewById(R.id.add_fire).setOnClickListener(this);
        findViewById(R.id.add_felling).setOnClickListener(this);
        findViewById(R.id.add_garbage).setOnClickListener(this);
//        findViewById(R.id.add_misc).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setMarginsToFAB(isMapShown());
    }

    private void setMarginsToFAB(final boolean add) {
        final View fab = findViewById(R.id.multiple_actions);
        final View status = findViewById(R.id.fl_status_panel);

        if (fab == null || status == null)
            return;

        final RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) fab.getLayoutParams();
        fab.post(new Runnable() {
            @Override
            public void run() {
                int bottom;
                if (add)
                    bottom = status.getMeasuredHeight();
                else
                    bottom = (int) UiUtil.dpToPx(MainActivity.this, 5);

                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottom);
                fab.setLayoutParams(lp);
                fab.requestLayout();
            }
        });
    }

    public void animateFAB(float from, float to, final boolean isHidden) {
        final FloatingActionsMenu fab = (FloatingActionsMenu) findViewById(R.id.multiple_actions);

        if (to == 1 && fab.getVisibility() == View.VISIBLE ||
                to == 0 && fab.getVisibility() == View.GONE)
            return;

        fab.collapse();
        int orientation = getResources().getConfiguration().orientation;
        float x = orientation == Configuration.ORIENTATION_PORTRAIT ? fab.getWidth() / 2 : fab.getWidth() - fab.getHeight() / 2;
        float y = orientation == Configuration.ORIENTATION_PORTRAIT ? fab.getHeight() - fab.getWidth() / 2 : fab.getHeight() / 2;
        ViewHelper.setPivotX(fab, x);
        ViewHelper.setPivotY(fab, y);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(fab, "scaleX", from, to),
                ObjectAnimator.ofFloat(fab, "scaleY", from, to)
        );

        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!isHidden)
                    fab.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isHidden)
                    fab.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        set.setDuration(300).start();
    }

    @Override
    public void onClick(View v) {
        FloatingActionsMenu fab = (FloatingActionsMenu) findViewById(R.id.multiple_actions);

        switch (v.getId()) {
            case R.id.grant_permissions:
                requestPermissions();
                break;
            case R.id.showcase_button:
                showLabels();
                fab.toggle();
                break;
            case R.id.fab_expand_menu_button:
                ShowcaseView sv = (ShowcaseView) findViewById(R.id.showcase);
                if (sv != null && sv.getVisibility() == View.VISIBLE)
                    showLabels();
                else
                    removeLabels();

                fab.toggle();
                break;
            case R.id.call:
                call();
                break;
            case R.id.add_fire:
                newMessage(Constants.MSG_TYPE_FIRE);
                break;
            case R.id.add_felling:
                newMessage(Constants.MSG_TYPE_FELLING);
                break;
            case R.id.add_garbage:
                newMessage(Constants.MSG_TYPE_GARBAGE);
                break;
//            case R.id.add_misc:
//                newMessage(Constants.MSG_TYPE_MISC);
//                break;
        }
    }

    private void showLabels() {
        ShowcaseView sv = (ShowcaseView) findViewById(R.id.showcase);
        sv.onClick(sv);

        FloatingActionsMenu fab = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        fab.setLabelsStyle(R.style.menu_labels_style);
        fab.createLabels();
    }

    private void removeLabels() {
        FloatingActionsMenu fab = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        fab.setLabelsStyle(0);
        fab.removeLabels();
    }

    public boolean isMapShown() {
        return mTabLayout != null && mTabLayout.getSelectedTabPosition() == 1;
    }

    private void newMessage(int type) {
        Intent intent = new Intent(this, CreateMessageActivity.class);
        intent.putExtra(Constants.FIELD_MTYPE, type);
        startActivity(intent);
    }

    private void call() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        VectorLayer regions = (VectorLayer) MapBase.getInstance().getLayerByName(Constants.KEY_FV_REGIONS);
        String region = prefs.getLong(SettingsConstants.KEY_PREF_REGION, -1L) + "";
        Cursor phone = regions.query(new String[]{Constants.FIELD_PHONE}, Constants.FIELD_ID + " = ?",
                new String[]{region}, null, null);

        if (phone.moveToFirst()) {
            String number = phone.getString(0).trim();

            if (!TextUtils.isEmpty(number)) {
                Uri call = Uri.parse("tel:" + number);
                Intent dialerIntent = new Intent(Intent.ACTION_DIAL, call);
                startActivity(dialerIntent);
            } else
                Toast.makeText(this, R.string.region_has_no_phone, Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(this, R.string.no_region_selected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddAccount(Account account, String token, boolean accountAdded) {
        if(accountAdded) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            float minX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINX, -2000.0f);
            float minY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINY, -2000.0f);
            float maxX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXX, 2000.0f);
            float maxY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXY, 2000.0f);

            final MainApplication app = (MainApplication) getApplication();
            app.setUserData(account.name, Constants.KEY_IS_AUTHORIZED, token);
            app.setUserData(account.name, SettingsConstants.KEY_PREF_USERMINX, "" + minX);
            app.setUserData(account.name, SettingsConstants.KEY_PREF_USERMINY, "" + minY);
            app.setUserData(account.name, SettingsConstants.KEY_PREF_USERMAXX, "" + maxX);
            app.setUserData(account.name, SettingsConstants.KEY_PREF_USERMAXY, "" + maxY);

            //free any map data here
            MapBase map = app.getMap();

            // delete all layers from map if any
            map.delete();

            //set sync with server
            ContentResolver.setSyncAutomatically(account, app.getAuthority(), true);
            ContentResolver.addPeriodicSync( account, app.getAuthority(), Bundle.EMPTY,
                    com.nextgis.maplib.util.Constants.DEFAULT_SYNC_PERIOD);

            // goto step 2
            refreshActivityView();
        } else
            Toast.makeText(this, R.string.error_init, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_settings).setVisible(!mFirstRun);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mViewPager != null) {
            if (BuildConfig.DEBUG)
                menu.findItem(R.id.action_sync).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        final MainApplication app = (MainApplication) getApplication();

        switch (id) {
            case R.id.action_sync:
                new Thread() {
                    @Override
                    public void run() {
                        testSync();
                    }
                }.start();
                return true;

            case R.id.action_settings:
                app.showSettings(null);
                return true;

            case R.id.action_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void testSync()
    {
        IGISApplication application = (IGISApplication) getApplication();
        MapBase map = application.getMap();
        NGWVectorLayer ngwVectorLayer;
        for (int i = 0; i < map.getLayerCount(); i++) {
            ILayer layer = map.getLayer(i);
            if (layer instanceof NGWVectorLayer) {
                ngwVectorLayer = (NGWVectorLayer) layer;
                ngwVectorLayer.sync(application.getAuthority(), new SyncResult());
            }
        }
    }

    @Override
    protected void setToolbar(int toolbarId){
        Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        toolbar.getBackground().setAlpha(getToolbarAlpha());
        setSupportActionBar(toolbar);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        protected MapFragment mMapFragment;
        protected MessageListFragment mMessageFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate or find existed fragment for the given page.
            String tag = getFragmentTag(mViewPager.getId(), position);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);

            if (fragment == null) {
                switch (position) {
                    case 0:
                        mMessageFragment = new MessageListFragment();
                        return mMessageFragment;
                    case 1:
                        mMapFragment = new MapFragment();
                        return mMapFragment;
                }
            }

            return fragment;
        }

        public String getFragmentTag(int viewPagerId, int fragmentPosition) {
            return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
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
                    return getString(R.string.title_notes).toUpperCase(l);
                case 1:
                    return getString(R.string.title_map).toUpperCase(l);
            }
            return null;
        }
    }

    public void setZoomAndCenter(float zoom, GeoPoint center) {
        mSectionsPagerAdapter.mMapFragment.setZoomAndCenter(zoom, center);
    }

    public void showMap() {
        mViewPager.setCurrentItem(1, true);
        mTabLayout.setScrollPosition(1, 0, true);

    }
}
