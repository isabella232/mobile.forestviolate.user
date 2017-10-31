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

package com.nextgis.safeforest;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.SettingsConstants;
import com.nextgis.maplibui.GISApplication;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.safeforest.util.MapUtil;

import java.io.File;

import static com.nextgis.maplib.util.Constants.MAP_EXT;
import static com.nextgis.maplib.util.SettingsConstants.KEY_PREF_MAP;


/**
 * The main application class stored some singleton objects.
 */
public class MainApplication
        extends GISApplication
{
    @Override
    public MapBase getMap()
    {
        if (null != mMap) {
            return mMap;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        File defaultPath = getExternalFilesDir(KEY_PREF_MAP);
        if (defaultPath == null) {
            defaultPath = new File(getFilesDir(), KEY_PREF_MAP);
        }

        String mapPath = sharedPreferences.getString(
                SettingsConstants.KEY_PREF_MAP_PATH, defaultPath.getPath());
        String mapName =
                sharedPreferences.getString(SettingsConstantsUI.KEY_PREF_MAP_NAME, "default");

        File mapFullPath = new File(mapPath, mapName + MAP_EXT);

        final Bitmap bkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI());
        mMap.setName(mapName);
        mMap.load();
        mMap.setMaxZoom(19);
        MapUtil.setMessageRenderer(mMap, this);

        return mMap;
    }


    /**
     * @return A authority for sync purposes or empty string if not sync anything
     */
    @Override
    public String getAuthority()
    {
        return com.nextgis.safeforest.util.SettingsConstants.AUTHORITY;
    }


    /**
     * Show settings Activity
     */
    @Override
    public void showSettings(String setting) { }

    @Override
    public void sendEvent(
            String category,
            String action,
            String label)
    {

    }

    @Override
    public void sendScreen(String name)
    {

    }

    @Override
    public String getAccountsType()
    {
        return com.nextgis.maplib.util.Constants.NGW_ACCOUNT_TYPE;
    }


    @Override
    protected int getThemeId(boolean isDark)
    {
        if (isDark) {
            return R.style.AppTheme_Dark;
        } else {
            return R.style.AppTheme_Light;
        }
    }
}
