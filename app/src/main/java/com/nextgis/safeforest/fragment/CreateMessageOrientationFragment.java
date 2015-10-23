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

package com.nextgis.safeforest.fragment;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.nextgis.safeforest.R;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.IMessage;


public class CreateMessageOrientationFragment
        extends Fragment
        implements IMessage
{
    protected EditText mMessage;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_create_message_orientation, container, false);
        mMessage = (EditText) view.findViewById(R.id.message);

        return view;
    }

    @Override
    public ContentValues getMessageData()
    {
        ContentValues values = new ContentValues();

        values.put(Constants.FIELD_MDATE, System.currentTimeMillis());
        values.put(Constants.FIELD_MESSAGE, mMessage.getText().toString());

        return values;
    }

}
