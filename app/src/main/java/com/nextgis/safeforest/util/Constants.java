/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:  NikitaFeodonit, nfeodonit@yandex.com
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
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

package com.nextgis.safeforest.util;

/**
 * Constants
 */
public interface Constants
{

    String SFTAG                        = "safe forest";
    int    MAX_DOCUMENTS                = 100;
    String TEMP_DOCUMENT_FEATURE_FOLDER = "temp_document";
    String ANONYMOUS                    = "anonymous";

    /**
     * State constants
     */
    int STEP_STATE_WAIT  = 0;
    int STEP_STATE_WORK  = 1;
    int STEP_STATE_DONE  = 2;
    int STEP_STATE_ERROR = 3;

    // BUNDLE
    String KEY_LOCATION = "location";

    // STATUS
    String KEY_STEP = "sync_step";
    String KEY_STATE = "sync_state";
    String KEY_MESSAGE = "sync_message";

    /**
     * NGW layer keys
     */
    String KEY_CITIZEN_MESSAGES = "citizen_messages";
    String KEY_FV_REGIONS = "fv_regions";
    String KEY_FV_FOREST = "fv_scan";
    String KEY_IS_AUTHORIZED = "is_authorised";

    /**
     * Threads
     */
    int DOWNLOAD_SEPARATE_THREADS = 10;

    String BROADCAST_MESSAGE = "sync_message";

    String FRAGMENT_SYNC_REGION      = "ngw_sync_region";
    String FRAGMENT_PREFERENCES      = "sf_preferences";
    String FRAGMENT_VIEW_MESSAGE     = "view_message";
    String FRAGMENT_USER_DATA_DIALOG = "user_data_dialog";
    String FRAGMENT_SELECT_LOCATION  = "select_location";

    String FIELD_ID        = "_id";
    String FIELD_MDATE     = "mdate";
    String FIELD_AUTHOR    = "author";
    String FIELD_CONTACT   = "contact";
    String FIELD_STATUS    = "status";
    String FIELD_MTYPE     = "mtype";
    String FIELD_MESSAGE   = "message";
    String FIELD_STMESSAGE = "stmessage";
    String FIELD_NAME      = "NAME_";
    String FIELD_PHONE     = "PHONE";

    int MSG_STATUS_UNKNOWN      = 0;
    int MSG_STATUS_NEW          = 1;
    int MSG_STATUS_SENT         = 2;
    int MSG_STATUS_ACCEPTED     = 3;
    int MSG_STATUS_NOT_ACCEPTED = 4;
    int MSG_STATUS_CHECKED      = 5;

    int MSG_TYPE_UNKNOWN = 0;
    int MSG_TYPE_FIRE    = 1;
    int MSG_TYPE_FELLING = 2;

    int MAX_LOCATION_MEASURES = 5;
    float MAX_LOCATION_ACCURACY = 5f;
    long MAX_LOCATION_TIME = 30000L;

    /**
     * Patterns
     */
//    String PHONE_PATTERN = "^\\+[0-9]+\\s\\(?[0-9]*\\)?\\s[0-9\\-]+$";
    String PHONE_PATTERN = "^((\\d{2,4})|([+1-9]+\\d{1,2}))?[-\\s]?"
            + "(\\d{3,4})?[-\\s]?((\\d{5,7})|(\\d{3}[-\\s]\\d{2}[-\\s]?\\d{2}))$";
    String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
}
