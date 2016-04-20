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

package com.nextgis.safeforest.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nextgis.maplibui.activity.NGActivity;
import com.nextgis.safeforest.BuildConfig;
import com.nextgis.safeforest.R;


public class AboutActivity extends NGActivity implements View.OnClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        setToolbar(R.id.main_toolbar);

        TextView txtVersion = (TextView) findViewById(R.id.app_version);
        txtVersion.setText(String.format(getString(R.string.version), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        ImageView logo = (ImageView) findViewById(R.id.logo);
        logo.setOnClickListener(this);

        Button credits = (Button) findViewById(R.id.credits);
        credits.setOnClickListener(this);

        TextView txtCopyrightText = (TextView) findViewById(R.id.copyright);
        txtCopyrightText.setText(Html.fromHtml(getString(R.string.copyright)));
        txtCopyrightText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.credits:
                AlertDialog builder = new AlertDialog.Builder(this, R.style.AppCompatDialog).setTitle(R.string.credits_intro)
                        .setMessage(R.string.credits)
                        .setPositiveButton(android.R.string.ok, null).create();
                builder.show();
                ((TextView) builder.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                ((TextView) builder.findViewById(android.R.id.message)).setLinksClickable(true);
                break;
            case R.id.logo:
                Intent url = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.wwf_link)));
                startActivity(url);
                break;
        }
    }
}
