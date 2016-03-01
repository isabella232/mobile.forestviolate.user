/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
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

package com.nextgis.safeforest.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.ViewMessageActivity;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.UiUtil;

import java.text.DateFormat;


public class MessageCursorAdapter
        extends CursorAdapter
        implements AdapterView.OnItemClickListener
{
    protected Context        mContext;
    protected LayoutInflater mInflater;

    protected int mIdColumn;
    protected int mDateColumn;
    protected int mAuthorColumn;
    protected int mStatusColumn;
    protected int mTypeColumn;
    protected int mMessageColumn;
    protected int mMessageDataType;


    public MessageCursorAdapter(
            Context context,
            Cursor cursor,
            int flags)
    {
        super(context, cursor, flags);

        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setColumns(cursor);
    }


    @Override
    public View newView(
            Context context,
            Cursor cursor,
            ViewGroup viewGroup)
    {
        return mInflater.inflate(R.layout.item_message, viewGroup, false);
    }


    @Override
    public void bindView(
            View view,
            Context context,
            Cursor cursor)
    {
        if (null == cursor) {
            return;
        }

        ImageView typeIcon = (ImageView) view.findViewById(R.id.type_icon);
        int type = cursor.getInt(mMessageDataType);
        switch (type) {
            case 0:
                type = cursor.getInt(mTypeColumn);

                switch (type) {
                    case Constants.MSG_TYPE_UNKNOWN:
                    default:
                        break;
                    case Constants.MSG_TYPE_FIRE:
                        typeIcon.setImageResource(R.drawable.ic_fire_light);
                        break;
                    case Constants.MSG_TYPE_FELLING:
                        typeIcon.setImageResource(R.drawable.ic_axe_light);
                        break;
                    case Constants.MSG_TYPE_GARBAGE:
                        typeIcon.setImageResource(R.drawable.ic_garbage_light);
                        break;
                    case Constants.MSG_TYPE_MISC:
                        typeIcon.setImageResource(R.drawable.ic_misc_light);
                        break;
                }
                break;
            case 1:
                typeIcon.setImageResource(R.drawable.ic_doc_light);
                break;
        }

        TextView author = (TextView) view.findViewById(R.id.author);
        author.setText(cursor.getString(mAuthorColumn));

        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(cursor.getString(mMessageColumn));

        TextView dateView = (TextView) view.findViewById(R.id.date);
        dateView.setText(UiUtil.formatDate(cursor.getLong(mDateColumn), DateFormat.SHORT));

        // TODO: get status from database
        ImageView stateIcon = (ImageView) view.findViewById(R.id.state_icon);
        int id = cursor.getInt(mIdColumn);
        if (id >= com.nextgis.maplib.util.Constants.MIN_LOCAL_FEATURE_ID) {
            stateIcon.setImageResource(R.drawable.ic_new_mail_light);
        } else {
            stateIcon.setImageResource(R.drawable.ic_sent_mail_light);
        }
    }


    @Override
    public Cursor swapCursor(Cursor newCursor)
    {
        setColumns(newCursor);
        return super.swapCursor(newCursor);
    }


    protected void setColumns(Cursor cursor)
    {
        if (null != cursor) {
            mIdColumn = cursor.getColumnIndex(Constants.FIELD_ID);
            mDateColumn = cursor.getColumnIndex(Constants.FIELD_MDATE);
            mAuthorColumn = cursor.getColumnIndex(Constants.FIELD_AUTHOR);
            mStatusColumn = cursor.getColumnIndex(Constants.FIELD_STATUS);
            mTypeColumn = cursor.getColumnIndex(Constants.FIELD_MTYPE);
            mMessageColumn = cursor.getColumnIndex(Constants.FIELD_MESSAGE);
            mMessageDataType = cursor.getColumnIndex(Constants.FIELD_DATA_TYPE);
        }
    }


    @Override
    public void onItemClick(
            AdapterView<?> parent,
            View view,
            int position,
            long id)
    {
        Cursor cursor = (Cursor) getItem(position);

        if (null == cursor) {
            return;
        }

        Intent intent = new Intent(mContext, ViewMessageActivity.class);
        intent.putExtra(Constants.FIELD_ID, id);
        intent.putExtra(Constants.FIELD_DATA_TYPE, cursor.getInt(mMessageDataType));
        mContext.startActivity(intent);
    }
}
