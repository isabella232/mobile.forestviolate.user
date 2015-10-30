/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
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
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.nextgis.maplib.api.IProgressor;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.TileItem;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.NGException;
import com.nextgis.maplibui.mapui.NGWVectorLayerUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;

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
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent mMessageIntent) {
        return null;
    }

    class InitialSync extends Thread implements IProgressor {
        protected Map<String, Long> mKeys = new HashMap<>();
        protected Intent mMessageIntent;
        protected Account mAccount;
        protected MapBase mMap;

        protected String mProgressMessage;
        protected int mMaxProgress;
        protected int mStep;
        protected boolean mIsRegionsOnly;
        protected float mMinX, mMinY, mMaxX, mMaxY;

        public InitialSync(boolean isRegionsOnly) {
            mIsRegionsOnly = isRegionsOnly;

            if (isRegionsOnly)
                mKeys.put(Constants.KEY_FV_REGIONS, -1L);
            else
                mKeys.put(Constants.KEY_CITIZEN_MESSAGES, -1L);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RegionSyncService.this);
            mMinX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINX, -2000.0f);
            mMinY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMINY, -2000.0f);
            mMaxX = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXX, 2000.0f);
            mMaxY = prefs.getFloat(SettingsConstants.KEY_PREF_USERMAXY, 2000.0f);
        }

        public void run() {
            mMessageIntent = new Intent(Constants.BROADCAST_MESSAGE);
            // step 1: connect to server
            mStep = 0;
            final MainApplication app = (MainApplication) getApplication();
            mAccount = app.getAccount(getString(R.string.account_name));
            final String sLogin = app.getAccountLogin(mAccount);
            final String sPassword = app.getAccountPassword(mAccount);
            final String sURL = app.getAccountUrl(mAccount);
            mMap = app.getMap();

            if (null == sURL || null == sPassword || null == sLogin) {
                return;
            }

            Connection connection = new Connection("tmp", sLogin, sPassword, sURL);
            publishProgress(getString(R.string.connecting), Constants.STEP_STATE_WORK);

            if (!connection.connect(sLogin.equals(Constants.ANONYMOUS))) {
                publishProgress(getString(R.string.error_connect_failed), Constants.STEP_STATE_ERROR);
                return;
            } else {
                publishProgress(getString(R.string.connected), Constants.STEP_STATE_WORK);
            }

            if (isCanceled())
                return;

            // step 1: find keys
            publishProgress(getString(R.string.check_tables_exist), Constants.STEP_STATE_WORK);

            if (!MapUtil.checkServerLayers(connection, mKeys)) {
                publishProgress(getString(R.string.error_wrong_server), Constants.STEP_STATE_ERROR);
                return;
            } else {
                publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
            }

            if (isCanceled())
                return;

            // step 2: create base layers
            mStep = 1;

            if (mIsRegionsOnly)
                loadRegions();
            else
                loadRegion();

            if (isCanceled())
                return;

            //TODO: load additional tables

            mMap.save();

            mStep++;
            publishProgress(null, Constants.STEP_STATE_DONE);
        }

        public final void publishProgress(String message, int state) {
            mMessageIntent.putExtra(Constants.KEY_STEP, mStep);
            mMessageIntent.putExtra(Constants.KEY_STATE, state);
            mMessageIntent.putExtra(Constants.KEY_MESSAGE, message);
            sendBroadcast(mMessageIntent);
        }

        protected void loadRegions() {
            NGWVectorLayerUI ngwVectorLayer = new NGWVectorLayerUI(getApplicationContext(), mMap.createLayerStorage(Constants.KEY_FV_REGIONS));
            ngwVectorLayer.setName(Constants.KEY_FV_REGIONS);
            ngwVectorLayer.setRemoteId(mKeys.get(Constants.KEY_FV_REGIONS));
            ngwVectorLayer.setAccountName(mAccount.name);
            ngwVectorLayer.setSyncType(com.nextgis.maplib.util.Constants.SYNC_ALL);
            ngwVectorLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            ngwVectorLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

            try {
                ngwVectorLayer.createFromNGW(this);
                mMap.addLayer(ngwVectorLayer);
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

        protected void loadRegion() {
            createBasicLayers(mMap);

            if (isCanceled())
                return;

            // step 3: citizen messages
            mStep = 2;
            publishProgress(getString(R.string.working), Constants.STEP_STATE_WORK);

            if (!loadCitizenMessages(mKeys.get(Constants.KEY_CITIZEN_MESSAGES), mAccount.name, mMap)) {
                publishProgress(getString(R.string.error_unexpected), Constants.STEP_STATE_ERROR);
            } else {
                publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
            }
        }

        private boolean loadCitizenMessages(long resourceId, String accountName, MapBase map) {
            NGWVectorLayerUI ngwVectorLayer = new NGWVectorLayerUI(getApplicationContext(),
                    map.createLayerStorage(Constants.KEY_CITIZEN_MESSAGES));

            ngwVectorLayer.setName(Constants.KEY_CITIZEN_MESSAGES);
            ngwVectorLayer.setRemoteId(resourceId);
            ngwVectorLayer.setServerWhere(String.format(Locale.US, "bbox=%f,%f,%f,%f",
                    mMinX, mMinY, mMaxX, mMaxY));
            ngwVectorLayer.setVisible(true);
            ngwVectorLayer.setAccountName(accountName);
            ngwVectorLayer.setSyncType(com.nextgis.maplib.util.Constants.SYNC_ALL);
            ngwVectorLayer.setMinZoom(GeoConstants.DEFAULT_MIN_ZOOM);
            ngwVectorLayer.setMaxZoom(GeoConstants.DEFAULT_MAX_ZOOM);

            map.addLayer(ngwVectorLayer);

            try {
                ngwVectorLayer.createFromNGW(this);
            } catch (NGException | IOException | JSONException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        protected void createBasicLayers(MapBase map) {
            publishProgress(getString(R.string.working), Constants.STEP_STATE_WORK);

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
            GeoEnvelope extent = new GeoEnvelope(mMinX, mMaxX, mMinY, mMaxY);

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

            if (extent.isInit()) {
                //download
                try {
                    downloadTiles(ksLayer, extent, 5, 9);
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
            if (map instanceof MapDrawable && extent.isInit()) {
                ((MapDrawable) map).zoomToExtent(extent);
            }

            publishProgress(getString(R.string.done), Constants.STEP_STATE_DONE);
        }

        private void downloadTiles(final RemoteTMSLayerUI osmLayer, GeoEnvelope loadBounds, int zoomFrom, int zoomTo) throws InterruptedException {
            //download
            publishProgress(getString(R.string.form_tiles_list), Constants.STEP_STATE_WORK);
            final List<TileItem> tilesList = new LinkedList<>();
            for (int zoom = zoomFrom; zoom < zoomTo + 1; zoom++) {
                tilesList.addAll(com.nextgis.maplib.util.MapUtil.getTileItems(loadBounds, zoom, osmLayer.getTMSType()));
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
                                        osmLayer.downloadTile(tile);
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
            return !mIsRunning;
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
