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

package com.nextgis.safeforest.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.nextgis.safeforest.R;
import com.nextgis.safeforest.fragment.MapFragment;
import com.nextgis.safeforest.util.Constants;

public class MessageMapActivity extends SFActivity {
    protected MapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragment);
        setToolbar(R.id.main_toolbar);

        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        mMapFragment = (MapFragment) fm.findFragmentByTag(Constants.FRAGMENT_SELECT_LOCATION);
        if (mMapFragment == null) {
            mMapFragment = new MapFragment();
        }

        ft.replace(R.id.container, mMapFragment, Constants.FRAGMENT_SELECT_LOCATION);
        ft.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapFragment.setSelectedLocationVisible(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.action_save:
                mMapFragment.setSelectedLocationVisible(false);
                Location location = mMapFragment.getSelectedLocation();
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_LOCATION, location);
                setResult(RESULT_OK, intent);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
