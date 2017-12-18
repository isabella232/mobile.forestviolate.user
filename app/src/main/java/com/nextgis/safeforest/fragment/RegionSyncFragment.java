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
 * This program is distributed in the hope that it will be   useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.safeforest.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.NetworkUtil;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.SFActivity;
import com.nextgis.safeforest.adapter.InitStepListAdapter;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.MapUtil;
import com.nextgis.safeforest.util.RegionSyncService;
import com.nextgis.safeforest.util.SettingsConstants;

import java.util.Locale;

public class RegionSyncFragment extends Fragment {
    private final static int LOADER_ID_REGIONS = 0;
    protected IGISApplication mApp;
    protected InitStepListAdapter mAdapter;
    protected BroadcastReceiver mSyncStatusReceiver;
    protected Button mCancelButton;

    protected boolean mIsRegionSet;
    protected boolean mStarted = false;

    public interface onRegionReceive {
        void onRegionChosen(String regionName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mAdapter = new InitStepListAdapter(getActivity());
        mApp = (IGISApplication) getActivity().getApplication();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIsRegionSet = MapUtil.isRegionSet(preferences) && MapUtil.hasLayer(mApp.getMap(), Constants.KEY_FV_REGIONS);
    }

    protected static void startSyncService(Activity activity, boolean isRegionsOnly) {
        NetworkUtil networkUtil = new NetworkUtil(activity);

        if (networkUtil.isNetworkAvailable()) {
            Intent syncIntent = new Intent(activity, RegionSyncService.class);
            syncIntent.setAction(RegionSyncService.ACTION_START);
            syncIntent.putExtra(Constants.KEY_FV_REGIONS, isRegionsOnly);

            DeleteLayersTask prepare = new DeleteLayersTask(activity, syncIntent);
            prepare.execute();
        } else
            Toast.makeText(activity, R.string.error_network_unavailable, Toast.LENGTH_SHORT).show();
    }

    protected static void stopSyncService(Activity activity) {
        Intent syncIntent = new Intent(activity, RegionSyncService.class);
        syncIntent.setAction(RegionSyncService.ACTION_STOP);
        activity.startService(syncIntent);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_MESSAGE);
        getActivity().registerReceiver(mSyncStatusReceiver, intentFilter);
        startSync();
    }

    protected void startSync() {
        if (mIsRegionSet) {
            startSyncService(getActivity(), false);
            mStarted = true;
            mCancelButton.setText(R.string.cancel);
        } else {
            createChooseRegionDialog(getActivity(), new onRegionReceive() {
                @Override
                public void onRegionChosen(String regionName) {
                    mIsRegionSet = true;
                    startSync();
                }
            });
        }
    }

    @Override
    public void onDetach() {
        getActivity().unregisterReceiver(mSyncStatusReceiver);
        super.onDetach();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_initial_sync, container, false);

        ListView list = (ListView) view.findViewById(R.id.stepsList);
        list.setAdapter(mAdapter);

        mCancelButton = (Button) view.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCancelButton.getText().equals(getString(R.string.cancel))) {
                    stopSyncService(getActivity());
                    mCancelButton.setText(R.string.retry);
                    mStarted = false;
                    mAdapter.reset();
                } else {
                    startSync();
                    mCancelButton.setText(R.string.cancel);
                }
            }
        });

        mSyncStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!mIsRegionSet || !mStarted)
                    return;

                int step = intent.getIntExtra(Constants.KEY_STEP, 0);
                int state = intent.getIntExtra(Constants.KEY_STATE, 0);
                String message = intent.getStringExtra(Constants.KEY_MESSAGE);

                if (step > 4)
                    ((NGActivity) getActivity()).refreshActivityView();
                else
                    mAdapter.setMessage(step, state, message);
            }
        };

        return view;
    }

    public static void createChooseRegionDialog(final FragmentActivity activity, final onRegionReceive callback) {
        final ProgressDialog waitDownload;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            waitDownload = new ProgressDialog(activity, android.R.style.Theme_Material_Light_Dialog_Alert);
        else
            waitDownload = new ProgressDialog(activity);

        waitDownload.setMessage(activity.getString(R.string.sf_getting_regions));
        waitDownload.setCanceledOnTouchOutside(false);
        waitDownload.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                stopSyncService(activity);
            }
        });
        waitDownload.show();

        final MainApplication app = (MainApplication) activity.getApplication();
        boolean hasRegionsLayer = MapUtil.hasLayer(app.getMap(), Constants.KEY_FV_REGIONS);
        String language = Locale.getDefault().getLanguage();
        final String locale = Constants.FIELD_NAME + (TextUtils.isEmpty(language) ? "_EN" : language.toUpperCase());

        final LoaderManager.LoaderCallbacks<Cursor> callbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                ILayer regions = MapBase.getInstance().getLayerByName(Constants.KEY_FV_REGIONS);
                if (regions == null) {
                    return null;
                }
                Uri uri = Uri.parse("content://" + app.getAuthority() + "/" + regions.getPath().getName());
                return new CursorLoader(activity, uri, new String[]{locale, Constants.FIELD_ID}, null, null, locale + " ASC");
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
                waitDownload.dismiss();
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                long regionId = preferences.getLong(SettingsConstants.KEY_PREF_REGION, 0);
                int defaultPosition = 0;

                if (data.moveToFirst())
                    do {
                        if (data.getLong(1) == regionId) {
                            defaultPosition = data.getPosition();
                            break;
                        }
                    } while (data.moveToNext());

                data.moveToPosition(defaultPosition);
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity, ((SFActivity) activity).getDialogThemeId());
                dialog.setTitle(R.string.sf_region_select)
                        .setSingleChoiceItems(data, defaultPosition, locale, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                data.moveToPosition(which);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                VectorLayer regions = (VectorLayer) MapBase.getInstance().getLayerByName(Constants.KEY_FV_REGIONS);
                                String regionName = data.getString(0);
                                long id = data.getLong(1);
                                GeoGeometry geometry = regions.getGeometryForId(id);

                                SharedPreferences.Editor edit = preferences.edit();
                                GeoEnvelope env = geometry.getEnvelope();
                                edit.putFloat(SettingsConstants.KEY_PREF_USERMINX, (float) env.getMinX());
                                edit.putFloat(SettingsConstants.KEY_PREF_USERMINY, (float) env.getMinY());
                                edit.putFloat(SettingsConstants.KEY_PREF_USERMAXX, (float) env.getMaxX());
                                edit.putFloat(SettingsConstants.KEY_PREF_USERMAXY, (float) env.getMaxY());
                                edit.putString(SettingsConstants.KEY_PREF_REGION_NAME, regionName);
                                edit.putLong(SettingsConstants.KEY_PREF_REGION, id);
                                edit.commit();

                                if (callback != null)
                                    callback.onRegionChosen(regionName);
                            }
                        });
                dialog.show().setCanceledOnTouchOutside(false);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {

            }
        };

        if (!hasRegionsLayer) {
            startSyncService(activity, true);

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.BROADCAST_MESSAGE);
            BroadcastReceiver regionsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String message = intent.getStringExtra(Constants.KEY_MESSAGE);

                    if (message == null) {
                        activity.getSupportLoaderManager().restartLoader(LOADER_ID_REGIONS, null, callbacks);
                        activity.unregisterReceiver(this);
                    }
                }
            };
            activity.registerReceiver(regionsReceiver, intentFilter);
        } else {
            activity.getSupportLoaderManager().restartLoader(LOADER_ID_REGIONS, null, callbacks);
        }
    }

    protected static class DeleteLayersTask extends AsyncTask<Void, Void, Void> {
        private Activity mActivity;
        private Intent mIntent;

        DeleteLayersTask(Activity activity, Intent intent) {
            mActivity = activity;
            mIntent = intent;
        }

        @Override
        protected Void doInBackground(Void... params) {
            MapBase map = MapBase.getInstance();
            for (int i = 0; i < map.getLayerCount(); i++) {
                ILayer layer = map.getLayer(i);
                if (!layer.getName().equals(Constants.KEY_FV_REGIONS)) {
                    map.removeLayer(layer);
                    layer.delete();
                    i--;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mActivity.startService(mIntent);
        }
    }
}
