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
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.datasource.ngw.LayerWithStyles;
import com.nextgis.maplib.datasource.ngw.Resource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.NGWRasterLayer;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.mapui.VectorLayerUI;
import com.nextgis.safeforest.BuildConfig;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.mapui.MessageLayerUI;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

public class RegionSyncService extends Service {
    public static final String ACTION_START = "START_INITIAL_SYNC";
    public static final String ACTION_STOP = "STOP_INITIAL_SYNC";

    private Thread mThread;
    private volatile boolean mIsRunning;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_NOT_STICKY;

        switch (intent.getAction()) {
            case ACTION_START:
                if (!mIsRunning)
                    startSync(intent.getBooleanExtra(Constants.KEY_FV_REGIONS, false));
                break;
            case ACTION_STOP:
                stopSync();
                break;
        }

        return START_STICKY;
    }

    private void startSync(boolean isRegionsOnly) {
        mThread = new InitialSync(isRegionsOnly);
        mIsRunning = true;
        mThread.start();
    }

    private void stopSync() {
        mIsRunning = false;

        if (mThread != null && mThread.isAlive())
            mThread.interrupt();

        stopSelf();
    }

    @Override
    public void onDestroy() {
        mIsRunning = false;

        if (mThread != null && mThread.isAlive())
            mThread.interrupt();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent mMessageIntent) {
        return null;
    }

    class InitialSync extends Thread implements IProgressor {
        Map<String, Resource> mKeys = new HashMap<>();
        Intent mMessageIntent;
        Account mAccount;
        MapBase mMap;

        String mProgressMessage, mURL;
        int mMaxProgress;
        int mStep;
        boolean mIsRegionsOnly;
        float mMinX, mMinY, mMaxX, mMaxY;

        InitialSync(boolean isRegionsOnly) {
            mIsRegionsOnly = isRegionsOnly;

            if (isRegionsOnly)
                mKeys.put(Constants.KEY_FV_REGIONS, null);
            else {
                mKeys.put(Constants.KEY_CITIZEN_MESSAGES, null);
                mKeys.put(Constants.KEY_FV_FOREST, null);
                mKeys.put(Constants.KEY_FV_LV, null);
                mKeys.put(Constants.KEY_FV_ULV, null);
                mKeys.put(Constants.KEY_FV_DOCS, null);
                mKeys.put(Constants.KEY_LANDSAT, null);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RegionSyncService.this);
            mMinX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINX, -2000.0f);
            mMinY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINY, -2000.0f);
            mMaxX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXX, 2000.0f);
            mMaxY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXY, 2000.0f);
        }

        public void run() {
            while (!isCanceled()) {
                mMessageIntent = new Intent(Constants.BROADCAST_MESSAGE);
                // step 1: connect to server
                mStep = 0;
                final MainApplication app = (MainApplication) getApplication();
                mMap = app.getMap();

                if (!mIsRegionsOnly) {
                    mAccount = app.getAccount(getString(R.string.account_name));
                    final String sLogin = app.getAccountLogin(mAccount);
                    final String sPassword = app.getAccountPassword(mAccount);
                    mURL = app.getAccountUrl(mAccount);

                    if (null == mURL || null == sPassword || null == sLogin) {
                        break;
                    }

                    Connection connection = new Connection("tmp", sLogin, sPassword, mURL);
                    publishProgress(getString(R.string.connecting), Constants.STEP_STATE_WORK);

                    if (!connection.connect(sLogin.equals(Constants.ANONYMOUS))) {
                        publishProgress(getString(R.string.error_connect_failed), Constants.STEP_STATE_ERROR);
                        break;
                    } else {
                        publishProgress(getString(R.string.connected), Constants.STEP_STATE_WORK);
                    }

                    if (isCanceled())
                        break;

                    // step 1: find keys
                    publishProgress(getString(R.string.check_tables_exist), Constants.STEP_STATE_WORK);

                    if (!MapUtil.checkServerLayers(connection, mKeys)) {
                        publishProgress(getString(R.string.error_wrong_server), Constants.STEP_STATE_ERROR);
                        break;
                    } else {
                        publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
                    }

                    if (isCanceled())
                        break;

                    // step 2: create base layers
                    mStep = 1;
                    loadRegion();
                    MapUtil.setMessageRenderer(mMap, app);
                } else
                    loadRegions();

                if (isCanceled())
                    break;

                mMap.save();
                mStep++;
                publishProgress(null, Constants.STEP_STATE_DONE);
                mIsRunning = false;
            }

            mIsRunning = false;
        }

        final void publishProgress(String message, int state) {
            mMessageIntent.putExtra(Constants.KEY_STEP, mStep);
            mMessageIntent.putExtra(Constants.KEY_STATE, state);
            mMessageIntent.putExtra(Constants.KEY_MESSAGE, message);
            sendBroadcast(mMessageIntent);
        }

        void loadRegions() {
            VectorLayerUI ngwVectorLayer = new VectorLayerUI(getApplicationContext(), mMap.createLayerStorage(Constants.KEY_FV_REGIONS));
            ngwVectorLayer.setName(Constants.KEY_FV_REGIONS);
            ngwVectorLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            ngwVectorLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
            Uri regions = Uri.parse(Constants.REGIONS_SERVERS);

            try {
                ngwVectorLayer.createFromGeoJson(regions, this);
                mMap.addLayer(ngwVectorLayer);
                mMap.save();
            } catch (NGException | IOException | JSONException e) {
                e.printStackTrace();
                ngwVectorLayer.delete();
                ngwVectorLayer = null;
            }

            if (ngwVectorLayer == null) {
                publishProgress(getString(R.string.error_unexpected), Constants.STEP_STATE_ERROR);
            } else {
                publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
            }
        }

        void loadRegion() {
            createBasicLayers(mMap);

            if (isCanceled())
                return;

            // step 3: citizen messages
            mStep = 2;
            publishProgress(getString(R.string.working), Constants.STEP_STATE_WORK);

            if (!loadNGWLayer(Constants.KEY_CITIZEN_MESSAGES, mAccount.name, mMap)) {
                publishProgress(getString(R.string.error_unexpected), Constants.STEP_STATE_ERROR);
            } else {
                publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
            }

            mStep = 3;
            publishProgress(getString(R.string.working), Constants.STEP_STATE_WORK);

            if (!loadNGWLayer(Constants.KEY_FV_FOREST, mAccount.name, mMap)) {
                publishProgress(getString(R.string.error_unexpected), Constants.STEP_STATE_ERROR);
            } else {
                publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
            }

            mStep = 4;
            publishProgress(getString(R.string.working), Constants.STEP_STATE_WORK);

            if (!loadNGWLayer(Constants.KEY_FV_DOCS, mAccount.name, mMap)) {
                publishProgress(getString(R.string.skip), Constants.STEP_STATE_ERROR);
            } else {
                publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
            }
        }

        private boolean loadNGWLayer(String layerName, String accountName, MapBase map) {
            NGWVectorLayerUI ngwVectorLayer;
            String date = Constants.FIELD_FV_DATE;
            if (layerName.equals(Constants.KEY_CITIZEN_MESSAGES)) {
                MessageLayerUI messageLayer = new MessageLayerUI(getApplicationContext(), map.createLayerStorage(layerName));
                messageLayer.setQueryRemoteId(mKeys.get(Constants.KEY_CITIZEN_FILTER_MESSAGES).getRemoteId());
                ngwVectorLayer = messageLayer;
                ngwVectorLayer.setVisible(true);
                date = Constants.FIELD_MDATE;
            } else
                ngwVectorLayer = new NGWVectorLayerUI(getApplicationContext(), map.createLayerStorage(layerName));

            ngwVectorLayer.setName(layerName);
            ngwVectorLayer.setRemoteId(mKeys.get(layerName).getRemoteId());
            ngwVectorLayer.setAccountName(accountName);
            ngwVectorLayer.setSyncType(com.nextgis.maplib.util.Constants.SYNC_ALL);
            ngwVectorLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            ngwVectorLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.MONTH, Constants.MONTH_TO_LOAD_DATA);
            ngwVectorLayer.setServerWhere(date + "={\"gt\":\"" + sdf.format(calendar.getTime()) + "T00:00:00Z\"}&" +
                    String.format(Locale.US, "bbox=%f,%f,%f,%f", mMinX, mMinY, mMaxX, mMaxY));

            map.addLayer(ngwVectorLayer);

            try {
                ngwVectorLayer.createFromNGW(this);
            } catch (NGException | IOException | JSONException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        void createBasicLayers(MapBase map) {
            publishProgress(getString(R.string.working), Constants.STEP_STATE_WORK);

            //add OpenStreetMap layer on application first run
            final RemoteTMSLayerUI osmLayer =
                    new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
            osmLayer.setName(SettingsConstants.OSM);
            osmLayer.setURL(SettingsConstants.OSM_URL);
            osmLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
            osmLayer.setMaxZoom(20);
            osmLayer.setMinZoom(11.4f);
            osmLayer.setVisible(true);
            map.addLayer(osmLayer);
            //mMap.moveLayer(0, osmLayer);
            GeoEnvelope extent = new GeoEnvelope(mMinX, mMaxX, mMinY, mMaxY);

        /*
        if(extent.isInit()) {
            try {
                downloadTiles(osmLayer, initAsyncTask, nStep, map.getFullBounds(), extent, 12, 13);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

            RemoteTMSLayerUI ksLayer = new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
            ksLayer.setName(SettingsConstants.KOSMOSNIMKI);
            ksLayer.setURL(SettingsConstants.KOSMOSNIMKI_URL);
            ksLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
            ksLayer.setMaxZoom(11.4f);
            ksLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            ksLayer.setVisible(true);
            map.addLayer(ksLayer);
            //mMap.moveLayer(1, ksLayer);

            if (extent.isInit()) {
                //download
                try {
                    downloadTiles(ksLayer, extent, 5, 7);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < SettingsConstants.LAYER_NAMES.length; i++) {
                RemoteTMSLayerUI layer = new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
                layer.setName(SettingsConstants.LAYER_NAMES[i]);
                layer.setURL(SettingsConstants.LAYER_URLS[i]);
                layer.setTMSType(GeoConstants.TMSTYPE_OSM);
                layer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
                layer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
                map.addLayer(layer);
            }

            long id = mKeys.get(Constants.KEY_LANDSAT).getRemoteId();
            NGWRasterLayer landsat = new NGWRasterLayer(getApplicationContext(), map.createLayerStorage());
            landsat.setName(Constants.KEY_LANDSAT);
            landsat.setAccountName(mAccount.name);
            landsat.setRemoteId(id);
            landsat.setURL(NGWUtil.getTMSUrl(mURL, new Long[]{id}));
            landsat.setTMSType(GeoConstants.TMSTYPE_OSM);
            landsat.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
            landsat.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            map.addLayer(landsat);

            String mixerLayerName = getString(R.string.geomixer_fv_tiles);
            String mixerLayerURL = SettingsConstants.VIOLATIONS_URL;
            RemoteTMSLayerUI mixerLayer =
                    new RemoteTMSLayerUI(getApplicationContext(), map.createLayerStorage());
            mixerLayer.setName(mixerLayerName);
            mixerLayer.setURL(mixerLayerURL);
            mixerLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
            mixerLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
            mixerLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            map.addLayer(mixerLayer);
            //mMap.moveLayer(2, mixerLayer);

            RemoteTMSLayer firesLayer = new RemoteTMSLayer(getApplicationContext(), map.createLayerStorage());
            firesLayer.setName(getString(R.string.fires));
            firesLayer.setURL(SettingsConstants.FIRES_URL);
            firesLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
            firesLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);
            firesLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            map.addLayer(firesLayer);

            long styleId = ((LayerWithStyles) mKeys.get(Constants.KEY_FV_LV)).getStyleId(0);
            NGWRasterLayer lvLayer = new NGWRasterLayer(getApplicationContext(), map.createLayerStorage());
            lvLayer.setName(getString(R.string.lv));
            lvLayer.setAccountName(mAccount.name);
            lvLayer.setRemoteId(styleId);
            lvLayer.setURL(NGWUtil.getTMSUrl(mURL, new Long[]{styleId}));
            lvLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
            lvLayer.setMaxZoom(9);
            lvLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            map.addLayer(lvLayer);

            if (extent.isInit()) {
                //download
                try {
                    downloadTiles(lvLayer, extent, 5, 8);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            styleId = ((LayerWithStyles) mKeys.get(Constants.KEY_FV_ULV)).getStyleId(0);
            NGWRasterLayer ulvLayer = new NGWRasterLayer(getApplicationContext(), map.createLayerStorage());
            ulvLayer.setName(getString(R.string.ulv));
            ulvLayer.setAccountName(mAccount.name);
            ulvLayer.setRemoteId(styleId);
            ulvLayer.setURL(NGWUtil.getTMSUrl(mURL, new Long[]{styleId}));
            ulvLayer.setTMSType(GeoConstants.TMSTYPE_OSM);
            ulvLayer.setMaxZoom(16);
            ulvLayer.setMinZoom(9);
            map.addLayer(ulvLayer);

            //set extent
            if (map instanceof MapDrawable && extent.isInit()) {
                ((MapDrawable) map).zoomToExtent(extent);
            }

            publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
        }

        private void downloadTiles(final RemoteTMSLayer layer, GeoEnvelope loadBounds, int zoomFrom, int zoomTo) throws InterruptedException {
            if (BuildConfig.DEBUG)
                return;
            //download
            publishProgress(getString(R.string.form_tiles_list), Constants.STEP_STATE_WORK);
            final List<TileItem> tilesList = new LinkedList<>();
            for (int zoom = zoomFrom; zoom < zoomTo + 1; zoom++) {
                tilesList.addAll(com.nextgis.maplib.util.MapUtil.getTileItems(loadBounds, zoom, layer.getTMSType()));
            }

            int threadCount = Constants.DOWNLOAD_SEPARATE_THREADS;
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                    threadCount, threadCount, com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME,
                    com.nextgis.maplib.util.Constants.KEEP_ALIVE_TIME_UNIT,
                    new LinkedBlockingQueue<Runnable>(), new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(
                        Runnable r,
                        ThreadPoolExecutor executor) {
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
                if (Thread.currentThread().isInterrupted() || isCanceled()) {
                    break;
                }

                final TileItem tile = tilesList.get(i);

                futures.add(
                        threadPool.submit(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        android.os.Process.setThreadPriority(
                                                com.nextgis.maplib.util.Constants.DEFAULT_DRAW_THREAD_PRIORITY);
                                        layer.downloadTile(tile);
                                    }
                                }));
            }

            // wait for download ending
            int nProgressStep = futures.size() / com.nextgis.maplib.util.Constants.DRAW_NOTIFY_STEP_PERCENT;
            if (nProgressStep == 0)
                nProgressStep = 1;
            double percentFract = 100.0 / futures.size();

            for (int i = 0, futuresSize = futures.size(); i < futuresSize; i++) {
                if (Thread.currentThread().isInterrupted() || isCanceled()) {
                    break;
                }

                try {
                    Future future = futures.get(i);
                    future.get(); // wait for task ending

                    if (i % nProgressStep == 0) {
                        int percent = (int) (i * percentFract);
                        publishProgress(percent + "% " + getString(R.string.downloaded), Constants.STEP_STATE_WORK);
                    }

                } catch (CancellationException | InterruptedException e) {
                    //e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void setMax(int maxValue) {
            mMaxProgress = maxValue;
        }

        @Override
        public boolean isCanceled() {
            return !mIsRunning || isInterrupted();
        }

        @Override
        public void setValue(int value) {
            publishProgress(mProgressMessage + " (" + value + " " + getString(R.string.of) + " " + mMaxProgress + ")", Constants.STEP_STATE_WORK);
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
