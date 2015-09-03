/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
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
public interface Constants {

    String SFTAG = "safe forest";
    int MAX_DOCUMENTS = 100;
    String TEMP_DOCUMENT_FEATURE_FOLDER = "temp_document";
    String ANONYMOUS = "anonymous";

    /**
     * State constants
     */
    int STEP_STATE_WAIT = 0;
    int STEP_STATE_WORK = 1;
    int STEP_STATE_DONE = 2;
    int STEP_STATE_ERROR = 3;

    /**
     * NGW layer keys
     */
    String KEY_CITIZEN_MESSAGES = "citizen_messages";

    /**
     * Threads
     */
    int DOWNLOAD_SEPARATE_THREADS = 10;
}
