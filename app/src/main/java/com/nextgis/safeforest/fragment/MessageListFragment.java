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

package com.nextgis.safeforest.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.activity.MainActivity;
import com.nextgis.safeforest.adapter.MessageCursorAdapter;
import com.nextgis.safeforest.util.Constants;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.io.IOException;


public class MessageListFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int LIST_LOADER = 12321;

    protected MessageCursorAdapter mAdapter;
    protected BroadcastReceiver mReceiver;
    protected IntentFilter mIntentFilter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        // Here we have to initialize the loader by reason the screen rotation
        getLoaderManager().initLoader(LIST_LOADER, null, this);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mAdapter = new MessageCursorAdapter(getContext(), null, 0);

        // register events from layers modify in services or other applications
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_DELETE);
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_DELETE_ALL);
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_INSERT);
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_UPDATE);
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_UPDATE_ALL);
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_UPDATE_FIELDS);
        mIntentFilter.addAction(com.nextgis.maplib.util.Constants.NOTIFY_FEATURE_ID_CHANGE);

        mReceiver = new VectorLayerNotifyReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        getContext().unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_message_list, container, false);

        ListView list = (ListView) rootView.findViewById(R.id.message_list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(mAdapter);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                if (null == cursor)
                    return false;

                try {
                    GeoMultiPoint point = (GeoMultiPoint) GeoGeometryFactory.fromBlob(cursor.getBlob(6));
                    if (point != null && point.size() > 0) {
                        ((MainActivity) getActivity()).setZoomAndCenter(15, point.get(0));
                        ((MainActivity) getActivity()).showMap();
                    }
                    return true;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int mLastFirstVisibleItem;
            private int mScrollState;
            private boolean mIsHidden = false;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                mScrollState = scrollState;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                FloatingActionsMenu fab = (FloatingActionsMenu) getActivity().findViewById(R.id.multiple_actions);
                float from = 0, to = 0;
                boolean needAnimation = false;

                switch (mScrollState) {
                    case SCROLL_STATE_FLING:
                    case SCROLL_STATE_TOUCH_SCROLL:
                        if (mLastFirstVisibleItem < firstVisibleItem && !mIsHidden) {
                            from = 1;
                            to = 0;
                            mIsHidden = true;
                            needAnimation = true;
                        }

                        if (mLastFirstVisibleItem > firstVisibleItem && mIsHidden) {
                            from = 0;
                            to = 1;
                            mIsHidden = false;
                            needAnimation = true;
                        }

                        break;
                }

                if (needAnimation) {
                    fab.collapse();
                    ViewHelper.setPivotX(fab, fab.getWidth() / 2);
                    ViewHelper.setPivotY(fab, fab.getHeight() - fab.getWidth() / 2);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(
                            ObjectAnimator.ofFloat(fab, "scaleX", from, to),
                            ObjectAnimator.ofFloat(fab, "scaleY", from, to)
                    );
                    set.setDuration(300).start();
                }

                mLastFirstVisibleItem = firstVisibleItem;
            }
        });

        return rootView;
    }


    @Override
    public Loader<Cursor> onCreateLoader(
            int loaderID,
            Bundle bundle)
    {
        switch (loaderID) {
            case LIST_LOADER:
                MainApplication app = (MainApplication) getActivity().getApplicationContext();

                Uri uri = Uri.parse(
                        "content://" + app.getAuthority() + "/" + Constants.KEY_CITIZEN_MESSAGES);

                String[] projection = {
                        Constants.FIELD_ID,
                        Constants.FIELD_MDATE,
                        Constants.FIELD_AUTHOR,
                        Constants.FIELD_STATUS,
                        Constants.FIELD_MTYPE,
                        Constants.FIELD_MESSAGE,
                        com.nextgis.maplib.util.Constants.FIELD_GEOM};

                String sortOrder = Constants.FIELD_MDATE + " DESC";

                return new CursorLoader(getActivity(), uri, projection, null, null, sortOrder);

            default:
                return null;
        }
    }


    @Override
    public void onLoadFinished(
            Loader<Cursor> loader,
            Cursor cursor)
    {
        mAdapter.swapCursor(cursor);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        mAdapter.changeCursor(null);
    }


    public class VectorLayerNotifyReceiver
            extends BroadcastReceiver
    {
        @Override
        public void onReceive(
                Context context,
                Intent intent)
        {
            // extreme logging commented
            //Log.d(TAG, "Receive notify: " + intent.getAction());

            if(!intent.hasExtra(com.nextgis.maplib.util.Constants.NOTIFY_LAYER_NAME))
                return;

            switch (intent.getAction()) {
                case com.nextgis.maplib.util.Constants.NOTIFY_DELETE:
                case com.nextgis.maplib.util.Constants.NOTIFY_DELETE_ALL:
                case com.nextgis.maplib.util.Constants.NOTIFY_INSERT:
                case com.nextgis.maplib.util.Constants.NOTIFY_UPDATE:
                case com.nextgis.maplib.util.Constants.NOTIFY_UPDATE_ALL:
                case com.nextgis.maplib.util.Constants.NOTIFY_UPDATE_FIELDS:
                case com.nextgis.maplib.util.Constants.NOTIFY_FEATURE_ID_CHANGE:
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}
