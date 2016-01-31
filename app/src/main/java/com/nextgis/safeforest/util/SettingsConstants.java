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

package com.nextgis.safeforest.util;

public interface SettingsConstants
{
    String AUTHORITY             = "com.nextgis.safeforest.provider";
    String SITE_URL = "http://176.9.38.120/fv";
    String KOSMOSNIMKI_URL = "http://{a,b,c}.tile.cart.kosmosnimki.ru/rs/{z}/{x}/{y}.png";
    String VIOLATIONS_URL = "http://maps.kosmosnimki.ru/TileService.ashx?request=gettile&layername=96BBFFE869E14CE8B739874798E39B60&srs=EPSG:3857" +
            "&z={z}&x={x}&y={y}&format=png&apikey=84RY0J61QQ";
    String FIRES_URL = "http://maps.kosmosnimki.ru/TileService.ashx?request=gettile&layername=F2840D287CD943C4B1122882C5B92565&srs=EPSG:3857" +
            "&z={z}&x={x}&y={y}&format=png&apikey=84RY0J61QQ&map=NDFYK&StartDate={s}&EndDate={e}";

    String KEY_PREF_USERMINX = "user_minx";
    String KEY_PREF_USERMINY = "user_miny";
    String KEY_PREF_USERMAXX = "user_maxx";
    String KEY_PREF_USERMAXY = "user_maxy";
    String KEY_PREF_REGION   = "user_region";
    String KEY_PREF_REGION_NAME   = "user_region_name";
    String KEY_PREF_CHANGE_REGION        = "change_region";
    String KEY_PREF_CHANGE_ACCOUNT       = "change_account";
    String KEY_USER_FULLNAME = "user_full_name";
    String KEY_USER_PHONE = "user_phone";
    String KEY_USER_EMAIL = "user_email";
    String KEY_PREF_FILTER = "message_filter";

    /**
     * map preference
     */
    String KEY_PREF_COORD_FORMAT = "coordinates_format";

    /**
     * Preference key - not UI
     */
    String KEY_PREF_SCROLL_X      = "map_scroll_x";
    String KEY_PREF_SCROLL_Y      = "map_scroll_y";
    String KEY_PREF_ZOOM_LEVEL    = "map_zoom_level";
    String KEY_PREF_MAP_FIRST_VIEW = "map_first_view";
    String KEY_PREF_SHOW_LOCATION = "map_show_loc";
    String KEY_PREF_SHOW_INFO     = "map_show_info";

    /**
     * Preference keys - in UI
     */
    String KEY_PREF_USER_ID             = "user_id";
    String KEY_PREF_SHOW_LAYES_LIST     = "show_layers_list";
    String KEY_PREF_ACCURATE_LOC        = "accurate_coordinates_pick";
    String KEY_PREF_ACCURATE_GPSCOUNT   = "accurate_coordinates_pick_count";
    String KEY_PREF_ACCURATE_CE         = "accurate_type";
    String KEY_PREF_TILE_SIZE           = "map_tile_size";
    String KEY_PREF_SHOW_ZOOM_CONTROLS  = "show_zoom_controls";
}
