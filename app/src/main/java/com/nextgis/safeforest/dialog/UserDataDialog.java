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

package com.nextgis.safeforest.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import com.nextgis.safeforest.R;


public class UserDataDialog
        extends YesNoDialog
{
    protected EditText mEmail;
    protected EditText mContacts;

    protected String mEmailText;
    protected String mContactsText;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View view = View.inflate(getActivity(), R.layout.dialog_user_data, null);

        mEmail = (EditText) view.findViewById(R.id.email);
        mEmail.setText(mEmailText);

        mContacts = (EditText) view.findViewById(R.id.contacts);
        mContacts.setText(mContactsText);

        setTitle(R.string.user_contact_info);
        // TODO: change icon
        setIcon(R.drawable.ic_phone_dark);
        setView(view);
        setPositiveText(R.string.ok);

        return super.onCreateDialog(savedInstanceState);
    }


    public void setEmailText(String emailText)
    {
        mEmailText = emailText;
    }


    public String getEmailText()
    {
        return mEmail.getText().toString();
    }


    public void setContactsText(String contactsText)
    {
        mContactsText = contactsText;
    }


    public String getContactsText()
    {
        return mContacts.getText().toString();
    }
}
