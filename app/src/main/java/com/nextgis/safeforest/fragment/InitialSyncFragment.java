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
 * This program is distributed in the hope that it will be   useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.safeforest.fragment;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.adapter.InitStepListAdapter;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.InitialSyncService;
import com.nextgis.safeforest.util.SettingsConstants;

public class InitialSyncFragment extends Fragment {
    protected InitStepListAdapter mAdapter;
    protected BroadcastReceiver mSyncStatusReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mAdapter = new InitStepListAdapter(getActivity());

        IGISApplication app = (IGISApplication) getActivity().getApplication();
        Account account = app.getAccount(getString(R.string.account_name));
        //set properties from account
        String auth = app.getAccountUserData(account, Constants.KEY_IS_AUTHORIZED);
        final SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();

        edit.putBoolean(Constants.KEY_IS_AUTHORIZED, !auth.equals(Constants.ANONYMOUS));

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

        Intent initialSyncIntent = new Intent(getActivity(), InitialSyncService.class);
        initialSyncIntent.setAction(InitialSyncService.ACTION_START);
        getActivity().startService(initialSyncIntent);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_MESSAGE);
        getActivity().registerReceiver(mSyncStatusReceiver, intentFilter);
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
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_initial_sync, container, false);

        ListView list = (ListView) view.findViewById(R.id.stepsList);
        list.setAdapter(mAdapter);

        Button cancelButton = (Button) view.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent initialSyncIntent = new Intent(getActivity(), InitialSyncService.class);
                initialSyncIntent.setAction(InitialSyncService.ACTION_STOP);
                getActivity().startService(initialSyncIntent);
            }
        });

        mSyncStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int step = intent.getIntExtra(Constants.KEY_STEP, 0);
                int state = intent.getIntExtra(Constants.KEY_STATE, 0);
                String message = intent.getStringExtra(Constants.KEY_MESSAGE);

                if (step > 2) {
                    ((NGActivity) getActivity()).refreshActivityView();
                }else
                    mAdapter.setMessage(step, state, message);
            }
        };

        return view;
    }

}
