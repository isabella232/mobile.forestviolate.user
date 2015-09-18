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

package com.nextgis.safeforest.activity;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.INGWResource;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.datasource.ngw.ResourceGroup;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.MapUtil;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.adapter.InitStepListAdapter;
import com.nextgis.safeforest.fragment.LoginFragment;
import com.nextgis.safeforest.fragment.MapFragment;
import com.nextgis.safeforest.fragment.MessageListFragment;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.SettingsConstants;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends SFActivity implements NGWLoginFragment.OnAddAccountListener {

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
    protected boolean mFirsRun;
    protected InitStepListAdapter mAdapter;

    protected static final String KEY_IS_AUTHORIZED = "is_authorised";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if first run
        final MainApplication app = (MainApplication) getApplication();
        if(app == null){
            Log.d(Constants.SFTAG, "failed to get main application");
            // should never happen
            mFirsRun = true;
            createFirstStartView();
        }

        // get from properties if first time
        // and registered or guest user

        final Account account = app.getAccount(getString(R.string.account_name));
        if(account == null){
            Log.d(Constants.SFTAG, "No account" + getString(R.string.account_name) + " created. Run first step.");
            mFirsRun = true;
            createFirstStartView();
        }
        else {
            MapBase map = app.getMap();
            if(map.getLayerCount() <= 0)
            {
                Log.d(Constants.SFTAG, "Account" + getString(R.string.account_name) + " created. Run second step.");
                mFirsRun = true;
                createSecondStartView(account);
            }
            else {
                Log.d(Constants.SFTAG, "Account" + getString(R.string.account_name) + " created. Layers created. Run normal view.");
                mFirsRun = false;
                createNormalView();
            }
        }
    }


    protected void createFirstStartView(){
        setContentView(R.layout.activity_main_first);

        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.first_run));

        FragmentManager fm = getSupportFragmentManager();
        NGWLoginFragment ngwLoginFragment = (NGWLoginFragment) fm.findFragmentByTag("NGWLogin");

        if (ngwLoginFragment == null) {
            ngwLoginFragment = new LoginFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(com.nextgis.maplibui.R.id.login_frame, ngwLoginFragment, "NGWLogin");
            ft.commit();
        }
        ngwLoginFragment.setForNewAccount(true);
        ngwLoginFragment.setOnAddAccountListener(this);
    }

    protected void createSecondStartView(Account account){
        setContentView(R.layout.activity_main_second);

        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.initialization));

        IGISApplication app = (IGISApplication) getApplication();
        //set properties from account
        String auth = app.getAccountUserData(account, KEY_IS_AUTHORIZED);
        final SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(this).edit();

        if(auth.equals(Constants.ANONYMOUS)) {
            edit.putBoolean(KEY_IS_AUTHORIZED, false);
        }
        else {
            edit.putBoolean(KEY_IS_AUTHORIZED, true);
        }

        String sMinX = app.getAccountUserData(account, SettingsConstants.KEY_PREF_USERMINX);
        if(!TextUtils.isEmpty(sMinX)){
            float minX = Float.parseFloat(sMinX);
            edit.putFloat(SettingsConstants.KEY_PREF_USERMINX, minX);
        }

        String sMinY = app.getAccountUserData(account, SettingsConstants.KEY_PREF_USERMINY);
        if(!TextUtils.isEmpty(sMinY)){
            float minY = Float.parseFloat(sMinY);
            edit.putFloat(SettingsConstants.KEY_PREF_USERMINY, minY);
        }

        String sMaxX = app.getAccountUserData(account, SettingsConstants.KEY_PREF_USERMAXX);
        if(!TextUtils.isEmpty(sMaxX)){
            float maxX = Float.parseFloat(sMaxX);
            edit.putFloat(SettingsConstants.KEY_PREF_USERMAXX, maxX);
        }

        String sMaxY = app.getAccountUserData(account, SettingsConstants.KEY_PREF_USERMAXY);
        if(!TextUtils.isEmpty(sMaxY)){
            float maxY = Float.parseFloat(sMaxY);
            edit.putFloat(SettingsConstants.KEY_PREF_USERMAXY, maxY);
        }
        edit.commit();

        mAdapter = new InitStepListAdapter(this);

        ListView list = (ListView) findViewById(R.id.stepsList);
        list.setAdapter(mAdapter);

        final InitAsyncTask task = new InitAsyncTask(account);

        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.cancel(true);
            }
        });

        task.execute();
    }


    protected void createNormalView(){

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_main);

        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.app_name));

        // Create the adapter that will return a fragment for each of the primary sections of the
        // activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        if(tabLayout.getTabCount() < mSectionsPagerAdapter.getCount()) {
            // For each of the sections in the app, add a tab to the action bar.
            for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                // Create a tab with text corresponding to the page title defined by
                // the adapter. Also specify this Activity object, which implements
                // the TabListener interface, as the callback (listener) for when
                // this tab is selected.
                tabLayout.addTab(tabLayout.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)));
            }
        }

        final View call = findViewById(R.id.call);
        if (null != call) {
            call.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            call();
                        }
                    });
        }

        final View addFire = findViewById(R.id.add_fire);
        if (null != addFire) {
            addFire.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addFire();
                        }
                    });
        }

        final View addFelling = findViewById(R.id.add_felling);
        if (null != addFelling) {
            addFelling.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            addFelling();
                        }
                    });
        }
    }

    private void addFire() {
        Intent intent = new Intent(this, CreateMessageActivity.class);
        intent.putExtra(Constants.FIELD_MTYPE, Constants.MSG_TYPE_FIRE);
        startActivity(intent);
    }

    private void addFelling() {
        Intent intent = new Intent(this, CreateMessageActivity.class);
        intent.putExtra(Constants.FIELD_MTYPE, Constants.MSG_TYPE_FELLING);
        startActivity(intent);
    }

    private void call() {

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
            app.setUserData(account.name, KEY_IS_AUTHORIZED, token);
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
        }
        else
            Toast.makeText(this, R.string.error_init, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(!mFirsRun)
            getMenuInflater().inflate(R.menu.main, menu);
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
                app.showSettings();
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

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            if (position == 0) {
                return new MessageListFragment();
            } else {
                return new MapFragment();
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
                    return getString(R.string.title_notes).toUpperCase(l);
                case 1:
                    return getString(R.string.title_map).toUpperCase(l);
            }
            return null;
        }
    }


    protected boolean checkServerLayers(INGWResource resource, Map<String, Long> keys){
        if (resource instanceof Connection) {
            Connection connection = (Connection) resource;
            connection.loadChildren();
        }
        else if (resource instanceof ResourceGroup) {
            ResourceGroup resourceGroup = (ResourceGroup) resource;
            resourceGroup.loadChildren();
        }

        for(int i = 0; i < resource.getChildrenCount(); ++i){
            INGWResource childResource = resource.getChild(i);

            if(keys.containsKey(childResource.getKey()) && childResource instanceof Resource) {
                Resource ngwResource = (Resource) childResource;
                keys.put(ngwResource.getKey(), ngwResource.getRemoteId());
            }

            boolean bIsFill = true;
            for (Map.Entry<String, Long> entry : keys.entrySet()) {
                if(entry.getValue() <= 0){
                    bIsFill = false;
                    break;
                }
            }

            if(bIsFill){
                return true;
            }

            if(checkServerLayers(childResource, keys)){
                return true;
            }
        }

        boolean bIsFill = true;

        for (Map.Entry<String, Long> entry : keys.entrySet()) {
            if(entry.getValue() <= 0){
                bIsFill = false;
                break;
            }
        }

        return bIsFill;
    }


    protected void createBasicLayers(MapBase map, final InitAsyncTask initAsyncTask, final int nStep){

        initAsyncTask.publishProgress(getString(R.string.working), nStep, Constants.STEP_STATE_WORK);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float minX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINX, -2000.0f);
        float minY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINY, -2000.0f);
        float maxX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXX, 2000.0f);
        float maxY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXY, 2000.0f);

        //add OpenStreetMap layer on application first run
        String layerName = getString(R.string.osm);
        String layerURL = SettingsConstantsUI.OSM_URL;
        final RemoteTMSLayerUI osmLayer =
                new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
        osmLayer.setName(layerName);
        osmLayer.setURL(layerURL);
        osmLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
        osmLayer.setMaxZoom(20);
        osmLayer.setMinZoom(12.4f);
        osmLayer.setVisible(true);

        map.addLayer(osmLayer);
        //mMap.moveLayer(0, osmLayer);
        GeoEnvelope extent = new GeoEnvelope(minX, maxX, minY, maxY);

        /*
        if(extent.isInit()) {
            try {
                downloadTiles(osmLayer, initAsyncTask, nStep, map.getFullBounds(), extent, 12, 13);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        String kosmosnimkiLayerName = getString(R.string.topo);
        String kosmosnimkiLayerURL = SettingsConstants.KOSOSNIMKI_URL;
        RemoteTMSLayerUI ksLayer =
                new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
        ksLayer.setName(kosmosnimkiLayerName);
        ksLayer.setURL(kosmosnimkiLayerURL);
        ksLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
        ksLayer.setMaxZoom(12.4f);
        ksLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
        ksLayer.setVisible(true);

        map.addLayer(ksLayer);
        //mMap.moveLayer(1, ksLayer);

        if(extent.isInit()) {
            //download
            try {
                downloadTiles(ksLayer, initAsyncTask, nStep, extent, 5, 9);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String mixerLayerName = getString(R.string.geomixer_fv_tiles);
        String mixerLayerURL = SettingsConstants.VIOLATIONS_URL;
        RemoteTMSLayerUI mixerLayer =
                new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
        mixerLayer.setName(mixerLayerName);
        mixerLayer.setURL(mixerLayerURL);
        mixerLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
        mixerLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
        mixerLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
        mixerLayer.setVisible(true);

        map.addLayer(mixerLayer);
        //mMap.moveLayer(2, mixerLayer);

        //set extent
        if(map instanceof MapDrawable && extent.isInit()) {
            ((MapDrawable) map).zoomToExtent(extent);
        }

        initAsyncTask.publishProgress(getString(R.string.done), nStep, Constants.STEP_STATE_DONE);
    }


    private void downloadTiles(final RemoteTMSLayerUI osmLayer, final InitAsyncTask initAsyncTask, final int nStep, GeoEnvelope loadBounds, int zoomFrom, int zoomTo) throws InterruptedException {
        //download
        initAsyncTask.publishProgress(getString(R.string.form_tiles_list), nStep, Constants.STEP_STATE_WORK);
        final List<TileItem> tilesList = new LinkedList<>();
        for(int zoom = zoomFrom; zoom < zoomTo + 1; zoom++) {
            tilesList.addAll(MapUtil.getTileItems(loadBounds, zoom, osmLayer.getTMSType()));
        }

        int threadCount = Constants.DOWNLOAD_SEPARATE_THREADS;
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                threadCount, threadCount, com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME,
                com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler()
        {
            @Override
            public void rejectedExecution(
                    Runnable r,
                    ThreadPoolExecutor executor)
            {
                try {
                    executor.getQueue().put(r);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    //throw new RuntimeException("Interrupted while submitting task", e);
                }
            }
        });

        int tilesSize = tilesList.size();
        List<Future> futures = new ArrayList<>(tilesSize);

        for (int i = 0; i < tilesSize; ++i) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            final TileItem tile = tilesList.get(i);

            futures.add(
                    threadPool.submit(
                            new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    android.os.Process.setThreadPriority(
                                            com.nextgis.maplib.util.Constants.DEFAULT_DRAW_THREAD_PRIORITY);
                                    osmLayer.downloadTile(tile);
                                }
                            }));
        }

        // wait for download ending
        int nProgressStep = futures.size() / com.nextgis.maplib.util.Constants.DRAW_NOTIFY_STEP_PERCENT;
        if(nProgressStep == 0)
            nProgressStep = 1;
        double percentFract = 100.0 / futures.size();

        for (int i = 0, futuresSize = futures.size(); i < futuresSize; i++) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                Future future = futures.get(i);
                future.get(); // wait for task ending

                if(i % nProgressStep == 0) {
                    int percent = (int) (i * percentFract);
                    initAsyncTask.publishProgress(percent + "% " + getString(R.string.downloaded), nStep, Constants.STEP_STATE_WORK);
                }

            } catch (CancellationException | InterruptedException e) {
                //e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean loadCitizenMessages(long resourceId, String accountName, MapBase map, IProgressor progressor) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float minX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINX, -2000.0f);
        float minY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINY, -2000.0f);
        float maxX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXX, 2000.0f);
        float maxY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXY, 2000.0f);

        NGWVectorLayerUI ngwVectorLayer = new NGWVectorLayerUI(getApplicationContext(),
                map.createLayerStorage(Constants.KEY_CITIZEN_MESSAGES));

        ngwVectorLayer.setName(Constants.KEY_CITIZEN_MESSAGES);
        ngwVectorLayer.setRemoteId(resourceId);
        ngwVectorLayer.setServerWhere(String.format(Locale.US, "bbox=%f,%f,%f,%f",
                minX, minY, maxX, maxY));
        ngwVectorLayer.setVisible(true);
        ngwVectorLayer.setAccountName(accountName);
        ngwVectorLayer.setSyncType(com.nextgis.maplib.util.Constants.SYNC_ALL);
        ngwVectorLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
        ngwVectorLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

        map.addLayer(ngwVectorLayer);

        try {
            ngwVectorLayer.createFromNGW(progressor);
        } catch (NGException | IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static Fragment newInstance(int sectionNumber) {
            if(sectionNumber == 0) {
                PlaceholderFragment fragment = new PlaceholderFragment();
                Bundle args = new Bundle();
                args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(args);
                return fragment;
            }
            else{
                MapFragment fragment = new MapFragment();
                Bundle args = new Bundle();
                args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(args);
                return fragment;
            }
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


    /**
     * A async task to execute resources functions (connect, loadChildren, etc.) asynchronously.
     */
    protected class InitAsyncTask
            extends AsyncTask<Void, Integer, Boolean> implements IProgressor
    {
        protected String mMessage;
        protected Account mAccount;
        protected int mMaxProgress;
        protected String mProgressMessage;
        protected int mStep;


        public InitAsyncTask(Account account) {
            mAccount = account;
            mMaxProgress = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            // step 1: connect to server
            mStep = 0;
            int nTimeout = 4000;
            final MainApplication app = (MainApplication) getApplication();
            final String sLogin = app.getAccountLogin(mAccount);
            final String sPassword = app.getAccountPassword(mAccount);
            final String sURL = app.getAccountUrl(mAccount);

            if (null == sURL || null == sPassword || null == sLogin) {
                return false;
            }

            Connection connection = new Connection("tmp", sLogin, sPassword, sURL);
            publishProgress(getString(R.string.connecting), mStep, Constants.STEP_STATE_WORK);

            if(!connection.connect(sLogin.equals(Constants.ANONYMOUS))){
                publishProgress(getString(R.string.error_connect_failed), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else{
                publishProgress(getString(R.string.connected), mStep, Constants.STEP_STATE_WORK);
            }

            if(isCancelled())
                return false;

            // step 1: find keys

            publishProgress(getString(R.string.check_tables_exist), mStep, Constants.STEP_STATE_WORK);

            Map<String, Long> keys = new HashMap<>();
            keys.put(Constants.KEY_CITIZEN_MESSAGES, -1L);

            if(!checkServerLayers(connection, keys)){
                publishProgress(getString(R.string.error_wrong_server), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;

            // step 2: create base layers

            mStep = 1;
            MapBase map = app.getMap();

            createBasicLayers(map, this, mStep);

            if(isCancelled())
                return false;

            // step 3: citizen messages

            mStep = 2;

            publishProgress(getString(R.string.working), mStep, Constants.STEP_STATE_WORK);

            if (!loadCitizenMessages(keys.get(Constants.KEY_CITIZEN_MESSAGES), mAccount.name, map, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;



            //TODO: load additional tables

            map.save();

            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            InitStepListAdapter.InitStep step =
                    (InitStepListAdapter.InitStep) mAdapter.getItem(values[0]);
            step.mStepDescription = mMessage;
            step.mState = values[1];

            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(!result){
                //delete map
                final MainApplication app = (MainApplication) getApplication();
                String accName = mAccount.name;
                app.removeAccount(mAccount);

                for(int i = 0; i < 10; i++){
                    if(app.getAccount(accName) == null)
                        break;
                }
            }
            refreshActivityView();
        }

        public final void publishProgress(String message, int step, int state) {
            mMessage = message;
            publishProgress(step, state);

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void setMax(int maxValue) {
            mMaxProgress = maxValue;
        }

        @Override
        public boolean isCanceled() {
            return super.isCancelled();
        }

        @Override
        public void setValue(int value) {
            mMessage = mProgressMessage + " (" + value + " " + getString(R.string.of) + " " + mMaxProgress + ")";
            publishProgress(mStep, Constants.STEP_STATE_WORK);
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {

        }

        @Override
        public void setMessage(String message) {
            mProgressMessage = message;
        }
    }
}
