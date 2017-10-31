/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;

import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplibui.util.ControlHelper;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.fragment.RegionSyncFragment;
import com.nextgis.safeforest.util.SettingsConstants;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.nextgis.safeforest.util.SettingsConstants.KEY_PREF_SYNC_PERIOD_SEC_LONG;


/**
 * Application preference
 */
public class PreferencesActivity extends SFActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragment);
        setToolbar(R.id.main_toolbar);

        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        PreferenceFragment preferenceFragment = (PreferenceFragment) fm.findFragmentByTag(com.nextgis.safeforest.util.Constants.FRAGMENT_PREFERENCES);

        if (preferenceFragment == null)
            preferenceFragment = new PreferenceFragment();

        ft.replace(R.id.container, preferenceFragment, com.nextgis.safeforest.util.Constants.FRAGMENT_PREFERENCES);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && isRegionSyncStarted())
            return getSupportFragmentManager().popBackStackImmediate();
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (isRegionSyncStarted())
            getSupportFragmentManager().popBackStack();
        else
            super.onBackPressed();
    }

    protected boolean isRegionSyncStarted() {
        return getSupportFragmentManager().getBackStackEntryCount() > 0;
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);
            final SFActivity activity = (SFActivity) getActivity();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

            Preference changeRegion = findPreference(SettingsConstants.KEY_PREF_CHANGE_REGION);
            changeRegion.setSummary(preferences.getString(SettingsConstants.KEY_PREF_REGION_NAME, null));
            changeRegion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    RegionSyncFragment.createChooseRegionDialog(activity, new RegionSyncFragment.onRegionReceive() {
                        @Override
                        public void onRegionChosen(String regionName) {
                            preference.setSummary(regionName);

                            FragmentManager fm = activity.getSupportFragmentManager();
                            RegionSyncFragment regionSyncFragment =
                                    (RegionSyncFragment) fm.findFragmentByTag(com.nextgis.safeforest.util.Constants.FRAGMENT_SYNC_REGION);

                            if (regionSyncFragment == null)
                                regionSyncFragment = new RegionSyncFragment();

                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.container, regionSyncFragment, com.nextgis.safeforest.util.Constants.FRAGMENT_SYNC_REGION);
                            ft.addToBackStack(null).commit();

                            //noinspection ConstantConditions
                            activity.getSupportActionBar().setTitle(R.string.sync_region);
                        }
                    });
                    return true;
                }
            });

            CheckBoxPreference syncSwitch = (CheckBoxPreference) findPreference(SettingsConstantsUI.KEY_PREF_SYNC_PERIODICALLY);
            SharedPreferences settings = activity.getSharedPreferences(Constants.PREFERENCES, Constants.MODE_MULTI_PROCESS);
            long timeStamp = settings.getLong(com.nextgis.maplib.util.SettingsConstants.KEY_PREF_LAST_SYNC_TIMESTAMP, 0);
            if (timeStamp > 0)
                syncSwitch.setSummary(ControlHelper.getSyncTime(activity, timeStamp));

            ListPreference historyRange = (ListPreference) findPreference(SettingsConstants.KEY_PREF_HISTORY);
            historyRange.setSummary(historyRange.getEntry());
            historyRange.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int value = Integer.parseInt(newValue.toString());
                    int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                    CharSequence summary = ((ListPreference) preference).getEntries()[id];
                    preference.setSummary(summary);
                    preference.getSharedPreferences().edit().putLong(KEY_PREF_SYNC_PERIOD_SEC_LONG, value).commit();

                    MainApplication app = (MainApplication) activity.getApplication();
                    MapBase map = app.getMap();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.add(Calendar.WEEK_OF_YEAR, value);

                    String date = com.nextgis.safeforest.util.Constants.FIELD_FV_DATE;
                    String time = sdf.format(calendar.getTime());
                    NGWVectorLayer layer = (NGWVectorLayer) map.getLayerByName(com.nextgis.safeforest.util.Constants.KEY_FV_FOREST);
                    setLayerWhere(layer, date, time);
                    layer = (NGWVectorLayer) map.getLayerByName(com.nextgis.safeforest.util.Constants.KEY_FV_DOCS);
                    setLayerWhere(layer, date, time);
                    date = com.nextgis.safeforest.util.Constants.FIELD_MDATE;
                    layer = (NGWVectorLayer) map.getLayerByName(com.nextgis.safeforest.util.Constants.KEY_CITIZEN_MESSAGES);
                    setLayerWhere(layer, date, time);

                    return true;
                }
            });

            ListPreference syncPeriod = (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_SYNC_PERIOD);
            syncPeriod.setSummary(syncPeriod.getEntry());
            syncPeriod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    long value = Long.parseLong(newValue.toString());
                    int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                    CharSequence summary = ((ListPreference) preference).getEntries()[id];
                    preference.setSummary(summary);
                    preference.getSharedPreferences().edit().putLong(KEY_PREF_SYNC_PERIOD_SEC_LONG, value).commit();

                    MainApplication app = (MainApplication) activity.getApplication();
                    final Account account = app.getAccount(com.nextgis.safeforest.util.Constants.ACCOUNT_NAME);
                    ContentResolver.addPeriodicSync(account, app.getAuthority(), Bundle.EMPTY, value);

                    return true;
                }
            });

            ListPreference appTheme = (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_THEME);
            appTheme.setSummary(appTheme.getEntry());

            ListPreference lpCoordinateFormat = (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_COORD_FORMAT);
            lpCoordinateFormat.setSummary(lpCoordinateFormat.getEntry());
            lpCoordinateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int value = Integer.parseInt(newValue.toString());
                    CharSequence summary = ((ListPreference) preference).getEntries()[value];
                    preference.setSummary(summary);

                    String preferenceKey = preference.getKey() + "_int";
                    preference.getSharedPreferences().edit().putInt(preferenceKey, value).commit();

                    return true;
                }
            });

            Preference changeAccount = findPreference(SettingsConstants.KEY_PREF_CHANGE_ACCOUNT);
            changeAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent accountSettings = new Intent(activity, AccountActivity.class);
                    startActivity(accountSettings);
                    return true;
                }
            });

            ListPreference showLocation = (ListPreference) findPreference(SettingsConstantsUI.KEY_PREF_SHOW_CURRENT_LOC);
            showLocation.setSummary(showLocation.getEntry());
            showLocation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int id = ((ListPreference) preference).findIndexOfValue((String) newValue);
                    CharSequence summary = ((ListPreference) preference).getEntries()[id];
                    preference.setSummary(summary);
                    return true;
                }
            });
        }

        private void setLayerWhere(NGWVectorLayer layer, String date, String time) {
            if (layer != null) {
                String bbox = layer.getServerWhere();
                int id = bbox.indexOf("&bbox");
                bbox = id > -1 ? bbox.substring(id) : "";
                layer.setServerWhere(date + "={\"gt\":\"" + time + "T00:00:00Z\"}" + bbox);
            }
        }
    }
}
