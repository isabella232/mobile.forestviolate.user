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

package com.nextgis.safeforest.fragment;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.util.NGWUtil;
import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.maplibui.service.HTTPLoader;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.dialog.UserDataDialog;
import com.nextgis.safeforest.dialog.YesNoDialog;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.SettingsConstants;
import com.nextgis.safeforest.util.UiUtil;

import java.util.regex.Pattern;

/**
 * The login fragment to the forest violations server
 */
public class LoginFragment extends NGWLoginFragment {

    protected ProgressDialog mProgressDialog;
    protected Button   mSkipButton, mSignUpButton;
    protected String mFullNameText, mPhoneText;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable
            ViewGroup container,
            @Nullable
            Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);
        mURL = (EditText) view.findViewById(R.id.url);
        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);
        mSignInButton = (Button) view.findViewById(R.id.signin);
        mSignUpButton = (Button) view.findViewById(R.id.signup);
        mSkipButton = (Button) view.findViewById(R.id.skip);

        mURL.addTextChangedListener(new URLWatcher());
        mLogin.addTextChangedListener(new EmailWatcher());
        mPassword.addTextChangedListener(new PasswordWatcher());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mProgressDialog = new ProgressDialog(getActivity(), android.R.style.Theme_Material_Light_Dialog_Alert);
        else
            mProgressDialog = new ProgressDialog(getActivity());

        return view;
    }

    private void validatePassword(String password) {
        if (!Pattern.matches(Constants.PASSWORD_PATTERN, password) && password.length() > 0)
            ((TextInputLayout) mPassword.getParent()).setError(getString(R.string.error_weak_password));
        else
            ((TextInputLayout) mPassword.getParent()).setErrorEnabled(false);
    }

    private void validateEmail(String email) {
        if (!UiUtil.isEmailValid(email) && email.length() > 0)
            ((TextInputLayout) mLogin.getParent()).setError(getString(R.string.email_not_valid));
        else
            ((TextInputLayout) mLogin.getParent()).setErrorEnabled(false);
    }

    private void validateURL(String url) {
        if (!isValidURL(url) && url.length() > 0)
            ((TextInputLayout) mURL.getParent()).setError(getString(R.string.error_invalid_url));
        else
            ((TextInputLayout) mURL.getParent()).setErrorEnabled(false);
    }

    private boolean isValidURL(String url) {
        return android.util.Patterns.WEB_URL.matcher(url).matches();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mSignInButton.setOnClickListener(this);
        mSignUpButton.setOnClickListener(this);
        mSkipButton.setOnClickListener(this);
    }


    @Override
    public void onPause()
    {
        mSignInButton.setOnClickListener(null);
        mSignUpButton.setOnClickListener(null);
        mSkipButton.setOnClickListener(null);
        super.onPause();
    }


    @Override
    public void onClick(View v)
    {
        if (v.getId() != R.id.skip && !UiUtil.isEmailValid(mLogin.getText().toString())) {
            Toast.makeText(getActivity(), R.string.email_not_valid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidURL(mURL.getText().toString())) {
            Toast.makeText(getActivity(), R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (v.getId()) {
            case R.id.signin:
                mProgressDialog.setMessage(getString(R.string.signing_in));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();

                getLoaderManager().restartLoader(R.id.auth_token_loader, null, this);
                break;
            case R.id.signup:
                final UserDataDialog dialog = new UserDataDialog();

                dialog.setOnPositiveClickedListener(new YesNoDialog.OnPositiveClickedListener() {
                    @Override
                    public void onPositiveClicked() {
                        mFullNameText = dialog.getFullNameText();
                        mPhoneText = dialog.getPhoneText();

                        if (TextUtils.isEmpty(mFullNameText) || TextUtils.isEmpty(mPhoneText)) {
                            Toast.makeText(getActivity(), R.string.anonymous_hint, Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (!UiUtil.isPhoneValid(mPhoneText)) {
                            Toast.makeText(getActivity(), R.string.phone_not_valid, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        dialog.dismiss();

                        mProgressDialog.setMessage(getString(R.string.signing_up));
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.show();

                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                final boolean[] result = new boolean[1];

                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        result[0] = NGWUtil.signUp(mURL.getText().toString().trim(),
                                                mLogin.getText().toString(), mPassword.getText().toString(), null, null);
                                    }
                                });
                                t.start();

                                while (t.isAlive())
                                    SystemClock.sleep(300);

                                if (result[0]) {
                                    getLoaderManager().restartLoader(R.id.auth_token_loader, null, LoginFragment.this);

                                    mSignUpButton.setEnabled(false);
                                    mSignInButton.setEnabled(false);
                                    mSkipButton.setEnabled(false);
                                } else
                                    Toast.makeText(getActivity(), R.string.error_sign_up, Toast.LENGTH_LONG).show();

                                mProgressDialog.dismiss();
                            }
                        });
                    }
                });

                dialog.setOnNegativeClickedListener(new YesNoDialog.OnNegativeClickedListener() {
                    @Override
                    public void onNegativeClicked() {
                        dialog.dismiss();
                    }
                });

                dialog.hideEmailField();
                dialog.setKeepInstance(true);
                dialog.show(getFragmentManager(), Constants.FRAGMENT_USER_DATA_DIALOG);
                break;
            case R.id.skip:
                getLoaderManager().restartLoader(R.id.non_auth_token_loader, null, this);
                break;
        }
    }

    @Override
    public Loader<String> onCreateLoader(
            int id,
            Bundle args)
    {
        if (id == R.id.auth_token_loader) {
            return new HTTPLoader(
                    getActivity().getApplicationContext(), mURL.getText().toString().trim(),
                    mLogin.getText().toString(), mPassword.getText().toString());
        }
        else if (id == R.id.non_auth_token_loader) {
            return new HTTPLoader(
                    getActivity().getApplicationContext(), mURL.getText().toString().trim(),
                    null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(
            Loader<String> loader,
            String token)
    {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.length() > 0) {
                onTokenReceived(getString(R.string.account_name), token);
            } else {
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();

                mSignUpButton.setEnabled(true);
                mSignInButton.setEnabled(true);
                mSkipButton.setEnabled(true);
            }
        }
        else if(loader.getId() == R.id.non_auth_token_loader){
            onTokenReceived(getString(R.string.account_name), Constants.ANONYMOUS);
        }
    }

    @Override
    protected void updateButtonState()
    {
        if (checkEditText(mURL)) {
            mSkipButton.setEnabled(true);

            if (checkEditText(mLogin) && checkEditText(mPassword)) {
                mSignInButton.setEnabled(true);
                mSignUpButton.setEnabled(true);
            }
        }
    }

    public void onTokenReceived(
            String accountName,
            String token)
    {
        IGISApplication app = (IGISApplication) getActivity().getApplication();
        if(token.equals(Constants.ANONYMOUS)){
            if (mForNewAccount) {
                boolean accountAdded = app.addAccount(accountName, mURL.getText().toString(), Constants.ANONYMOUS, Constants.ANONYMOUS, token);
                if(accountAdded) {
                    if (null != mOnAddAccountListener) {
                        mOnAddAccountListener.onAddAccount(app.getAccount(accountName), token, true);
                    }

                    app.setUserData(accountName, SettingsConstants.KEY_USER_FULLNAME, mFullNameText);
                    app.setUserData(accountName, SettingsConstants.KEY_USER_PHONE, mPhoneText);
                }
                else {
                    if (null != mOnAddAccountListener) {
                        mOnAddAccountListener.onAddAccount(null, token, false);
                    }
                }
            } else {
                // do nothing, guest account cannot be changed
                getActivity().finish();
            }
        }
        else{
            if (mForNewAccount) {
                app.setUserData(accountName, SettingsConstants.KEY_USER_FULLNAME, mFullNameText);
                app.setUserData(accountName, SettingsConstants.KEY_USER_PHONE, mPhoneText);
            } else if (null != mOnAddAccountListener)
                mOnAddAccountListener.onAddAccount(app.getAccount(accountName), token, false);

            super.onTokenReceived(accountName, token);
        }
    }

    public class PasswordWatcher extends LocalTextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            super.afterTextChanged(s);
            validatePassword(s.toString());
        }
    }

    public class EmailWatcher extends LocalTextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            super.afterTextChanged(s);
            validateEmail(s.toString());
        }
    }

    public class URLWatcher extends LocalTextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            super.afterTextChanged(s);
            validateURL(s.toString());
        }
    }
}
