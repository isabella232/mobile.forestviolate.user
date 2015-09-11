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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class MessageCursorAdapter
        extends CursorAdapter
        implements AdapterView.OnItemClickListener
{
    protected Context mContext;
    protected LayoutInflater mInflater;

    protected int mDateColumn;
    protected int mAuthorColumn;
    protected int mStatusColumn;
    protected int mTypeColumn;
    protected int mMessageColumn;


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
        typeIcon.setImageResource(R.drawable.ic_axe_light);

        TextView author = (TextView) view.findViewById(R.id.author);
        author.setText(cursor.getString(mAuthorColumn));

        TextView message = (TextView) view.findViewById(R.id.message);
        message.setText(cursor.getString(mMessageColumn));

        TextView date = (TextView) view.findViewById(R.id.date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(cursor.getLong(mDateColumn));
        Date d = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy", Locale.US);
        date.setText(sdf.format(d));

        ImageView stateIcon = (ImageView) view.findViewById(R.id.state_icon);
        stateIcon.setImageResource(R.drawable.ic_new_mail_light);
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
            mDateColumn = cursor.getColumnIndex(Constants.FIELD_MDATE);
            mAuthorColumn = cursor.getColumnIndex(Constants.FIELD_AUTHOR);
            mStatusColumn = cursor.getColumnIndex(Constants.FIELD_STATUS);
            mTypeColumn = cursor.getColumnIndex(Constants.FIELD_MTYPE);
            mMessageColumn = cursor.getColumnIndex(Constants.FIELD_MESSAGE);
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
        mContext.startActivity(intent);
    }
}
