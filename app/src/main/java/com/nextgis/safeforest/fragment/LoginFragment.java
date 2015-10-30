/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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

package com.nextgis.safeforest.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.maplibui.service.HTTPLoader;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.util.Constants;

/**
 * The login fragment to the forest violations server
 */
public class LoginFragment extends NGWLoginFragment {

    protected Button   mSkipButton;

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
        mSkipButton = (Button) view.findViewById(R.id.skip);

        TextWatcher watcher = new LocalTextWatcher();
        mURL.addTextChangedListener(watcher);
        mLogin.addTextChangedListener(watcher);
        mPassword.addTextChangedListener(watcher);

        return view;
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mSignInButton.setOnClickListener(this);
        mSkipButton.setOnClickListener(this);
    }


    @Override
    public void onPause()
    {
        mSignInButton.setOnClickListener(null);
        mSkipButton.setOnClickListener(null);
        super.onPause();
    }


    @Override
    public void onClick(View v)
    {
        if (v == mSignInButton) {
            getLoaderManager().restartLoader(R.id.auth_token_loader, null, this);
        }
        else if (v == mSkipButton) {
            getLoaderManager().restartLoader(R.id.non_auth_token_loader, null, this);
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
        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.length() > 0) {
                onTokenReceived(getString(R.string.account_name), token);
            } else {
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();
            }
        }
        else if(loader.getId() == R.id.non_auth_token_loader){
            onTokenReceived(getString(R.string.account_name), Constants.ANONYMOUS);
        }
    }

    @Override
    protected void updateButtonState()
    {
        if (checkEditText(mURL)){
            mSkipButton.setEnabled(true);
            if( checkEditText(mLogin) && checkEditText(mPassword)) {
                mSignInButton.setEnabled(true);
            }
        }
    }

    public void onTokenReceived(
            String accountName,
            String token)
    {
        if(token.equals(Constants.ANONYMOUS)){
            IGISApplication app = (IGISApplication) getActivity().getApplication();

            if (mForNewAccount) {
                boolean accountAdded = app.addAccount(accountName, mURL.getText().toString(), Constants.ANONYMOUS, Constants.ANONYMOUS, token);
                if(accountAdded) {
                    if (null != mOnAddAccountListener) {
                        mOnAddAccountListener.onAddAccount(app.getAccount(accountName), token, accountAdded);
                    }
                }
                else {
                    if (null != mOnAddAccountListener) {
                        mOnAddAccountListener.onAddAccount(null, token, accountAdded);
                    }
                }

            } else {
                // do nothing, guest account cannot be changed
                getActivity().finish();
            }
        }
        else{
            super.onTokenReceived(accountName, token);
        }
    }
}
