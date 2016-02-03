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

package com.nextgis.safeforest.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.fragment.ViewMessageFragment;
import com.nextgis.safeforest.util.Constants;


public class ViewMessageActivity
        extends SFActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fragment);
        setToolbar(R.id.main_toolbar);

        final FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ViewMessageFragment viewMessageFragment =
                (ViewMessageFragment) fm.findFragmentByTag(Constants.FRAGMENT_VIEW_MESSAGE);

        if (viewMessageFragment == null) {
            viewMessageFragment = new ViewMessageFragment();
        }

        ft.replace(R.id.container, viewMessageFragment, Constants.FRAGMENT_VIEW_MESSAGE);
        ft.commit();
    }

    public void setTitle(String title) {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
    }
}
