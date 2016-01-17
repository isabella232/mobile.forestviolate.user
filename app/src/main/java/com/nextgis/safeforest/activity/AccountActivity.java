/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2016 NextGIS, info@nextgis.com
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.fragment.LoginFragment;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.SettingsConstants;

import java.util.regex.Pattern;

/**
 * Application preference
 */
public class AccountActivity extends SFActivity {
    protected MainApplication mApp;

    @Override
    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        setToolbar(R.id.main_toolbar);

        mApp = (MainApplication) getApplication();
        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment fragment;
        if (getIntent().getAction() != null && getIntent().getAction().equals(Constants.FRAGMENT_LOGIN)) {
            fragment = fm.findFragmentByTag(Constants.FRAGMENT_LOGIN);

            if (fragment == null)
                fragment = new LoginFragment();

            ((LoginFragment) fragment).setForNewAccount(false);
            ((LoginFragment) fragment).setOnAddAccountListener(new NGWLoginFragment.OnAddAccountListener() {
                @Override
                public void onAddAccount(Account account, String token, boolean accountAdded) {
                    if (account != null)
                        mApp.setUserData(account.name, Constants.KEY_IS_AUTHORIZED, token);
                }
            });
        } else {
            fragment = fm.findFragmentByTag(Constants.FRAGMENT_ACCOUNT);

            if (fragment == null)
                fragment = new AccountFragment();
        }

        ft.replace(R.id.container, fragment, Constants.FRAGMENT_ACCOUNT);
        ft.commit();
    }

    public static class AccountFragment extends PreferenceFragmentCompat {
        protected MainApplication mApp;
        protected Account mAccount;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.account);
            final SFActivity activity = (SFActivity) getActivity();
            mApp = (MainApplication) activity.getApplication();
            mAccount = mApp.getAccount(getString(R.string.account_name));

            EditTextPreference fullName = (EditTextPreference) findPreference(SettingsConstants.KEY_USER_FULLNAME);
            fullName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String fullName = (String) o;
                    if (TextUtils.isEmpty(fullName.trim())) {
                        Toast.makeText(activity, R.string.anonymous_hint, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    mApp.setUserData(mAccount.name, SettingsConstants.KEY_USER_FULLNAME, fullName);
                    preference.setSummary(fullName);
                    return true;
                }
            });

            EditTextPreference phone = (EditTextPreference) findPreference(SettingsConstants.KEY_USER_PHONE);
            phone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String phone = (String) o;
                    Pattern pattern = Pattern.compile(Constants.PHONE_PATTERN);
                    if (!pattern.matcher(phone).matches()) {
                        Toast.makeText(activity, R.string.phone_not_valid, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    mApp.setUserData(mAccount.name, SettingsConstants.KEY_USER_PHONE, phone);
                    preference.setSummary(phone);
                    return true;
                }
            });

            EditTextPreference email = (EditTextPreference) findPreference(SettingsConstants.KEY_USER_EMAIL);
            email.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String email = (String) o;
                    if (!TextUtils.isEmpty((email))) {
                        Pattern pattern = Pattern.compile(Constants.EMAIL_PATTERN);
                        if (!pattern.matcher(email).matches()) {
                            Toast.makeText(activity, R.string.email_not_valid, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }

                    mApp.setUserData(mAccount.name, SettingsConstants.KEY_USER_EMAIL, email);
                    preference.setSummary(email);
                    return true;
                }
            });

            Preference changeServer = findPreference(SettingsConstants.KEY_PREF_CHANGE_ACCOUNT);
            changeServer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent accountSettings = new Intent(activity, AccountActivity.class);
                    accountSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    accountSettings.setAction(Constants.FRAGMENT_LOGIN);
                    startActivity(accountSettings);
                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            String emailLogin = mApp.getAccountLogin(mAccount);

            PreferenceCategory login = (PreferenceCategory) findPreference(SettingsConstants.KEY_PREF_USER_ID);
            login.setTitle(emailLogin);

            EditTextPreference fullName = (EditTextPreference) findPreference(SettingsConstants.KEY_USER_FULLNAME);
            fullName.setSummary(mApp.getAccountUserData(mAccount, SettingsConstants.KEY_USER_FULLNAME));

            EditTextPreference phone = (EditTextPreference) findPreference(SettingsConstants.KEY_USER_PHONE);
            phone.setSummary(mApp.getAccountUserData(mAccount, SettingsConstants.KEY_USER_PHONE));

            EditTextPreference email = (EditTextPreference) findPreference(SettingsConstants.KEY_USER_EMAIL);
            email.setSummary(mApp.getAccountUserData(mAccount, SettingsConstants.KEY_USER_EMAIL));

            if (email.isVisible() && !emailLogin.equals(Constants.ANONYMOUS))
                email.setVisible(false);
            else if (!email.isVisible())
                email.setVisible(true);
        }
    }
}
