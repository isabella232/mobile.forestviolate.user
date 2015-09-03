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

package com.nextgis.safeforest.activity;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.datasource.ngw.Connection;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.safeforest.MainApplication;
import com.nextgis.safeforest.adapter.InitStepListAdapter;
import com.nextgis.safeforest.fragment.LoginFragment;
import com.nextgis.safeforest.fragment.MapFragment;
import com.nextgis.safeforest.R;
import com.nextgis.safeforest.util.Constants;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends SFActivity implements NGWLoginFragment.OnAddAccountListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    protected SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    protected ViewPager mViewPager;
    protected boolean mFirsRun;
    protected InitStepListAdapter mAdapter;

    protected static final String KEY_IS_AUTHORIZED = "is_authorised";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if first run
        final MainApplication app = (MainApplication) getApplication();
        if(app == null){
            Log.d(Constants.SFTAG, "failed to get main application");
            // should never happen
            mFirsRun = true;
            createFirstStartView();
        }

        // get from properties if first time
        // and registered or guest user

        final Account account = app.getAccount(getString(R.string.account_name));
        if(account == null){
            Log.d(Constants.SFTAG, "No account" + getString(R.string.account_name) + " created. Run first step.");
            mFirsRun = true;
            createFirstStartView();
        }
        else {
            MapBase map = app.getMap();
            if(map.getLayerCount() <= 0)
            {
                Log.d(Constants.SFTAG, "Account" + getString(R.string.account_name) + " created. Run second step.");
                mFirsRun = true;
                createSecondStartView(account);
            }
            else {
                Log.d(Constants.SFTAG, "Account" + getString(R.string.account_name) + " created. Layers created. Run normal view.");
                mFirsRun = false;
                createNormalView();
            }
        }
    }


    protected void createFirstStartView(){
        setContentView(R.layout.activity_main_first);

        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.first_run));

        FragmentManager fm = getSupportFragmentManager();
        NGWLoginFragment ngwLoginFragment = (NGWLoginFragment) fm.findFragmentByTag("NGWLogin");

        if (ngwLoginFragment == null) {
            ngwLoginFragment = new LoginFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(com.nextgis.maplibui.R.id.login_frame, ngwLoginFragment, "NGWLogin");
            ft.commit();
        }
        ngwLoginFragment.setForNewAccount(true);
        ngwLoginFragment.setOnAddAccountListener(this);
    }

    protected void createSecondStartView(Account account){
        setContentView(R.layout.activity_main_second);

        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.initialization));

        mAdapter = new InitStepListAdapter(this);

        ListView list = (ListView) findViewById(R.id.stepsList);
        list.setAdapter(mAdapter);

        final InitAsyncTask task = new InitAsyncTask(account);

        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.cancel(true);
            }
        });

        task.execute();
    }


    protected void createNormalView(){

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_main);

        setToolbar(R.id.main_toolbar);
        setTitle(getText(R.string.app_name));

        // Create the adapter that will return a fragment for each of the primary sections of the
        // activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        if(tabLayout.getTabCount() < mSectionsPagerAdapter.getCount()) {
            // For each of the sections in the app, add a tab to the action bar.
            for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
                // Create a tab with text corresponding to the page title defined by
                // the adapter. Also specify this Activity object, which implements
                // the TabListener interface, as the callback (listener) for when
                // this tab is selected.
                tabLayout.addTab(tabLayout.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)));
            }
        }

        final View call = findViewById(R.id.call);
        if (null != call) {
            call.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            call();
                        }
                    });
        }

        final View addFire = findViewById(R.id.add_fire);
        if (null != addFire) {
            addFire.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addFire();
                        }
                    });
        }

        final View addLogging = findViewById(R.id.add_logging);
        if (null != addLogging) {
            addLogging.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addLogging();
                        }
                    });
        }
    }

    private void addFire() {

    }

    private void addLogging() {

    }

    private void call() {

    }

    @Override
    public void onAddAccount(Account account, String token, boolean accountAdded) {
        if(accountAdded) {

            final SharedPreferences.Editor edit =
                    PreferenceManager.getDefaultSharedPreferences(this).edit();

            if(token.equals(Constants.ANONYMOUS)) {
                edit.putBoolean(KEY_IS_AUTHORIZED, false);
            }
            else {
                edit.putBoolean(KEY_IS_AUTHORIZED, false);
            }
            edit.commit();

            //free any map data here
            final MainApplication app = (MainApplication) getApplication();
            MapBase map = app.getMap();

            // delete all layers from map if any
            map.delete();

            //set sync with server
            ContentResolver.setSyncAutomatically(account, app.getAuthority(), true);
            ContentResolver.addPeriodicSync( account, app.getAuthority(), Bundle.EMPTY,
                    com.nextgis.maplib.util.Constants.DEFAULT_SYNC_PERIOD);

            // goto step 2
            refreshActivityView();
        }
        else
            Toast.makeText(this, R.string.error_init, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_about) {
            Intent intentAbout = new Intent(this, AboutActivity.class);
            startActivity(intentAbout);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void setToolbar(int toolbarId){
        Toolbar toolbar = (Toolbar) findViewById(toolbarId);
        toolbar.getBackground().setAlpha(getToolbarAlpha());
        setSupportActionBar(toolbar);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_notes).toUpperCase(l);
                case 1:
                    return getString(R.string.title_map).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static Fragment newInstance(int sectionNumber) {
            if(sectionNumber == 0) {
                PlaceholderFragment fragment = new PlaceholderFragment();
                Bundle args = new Bundle();
                args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(args);
                return fragment;
            }
            else{
                MapFragment fragment = new MapFragment();
                Bundle args = new Bundle();
                args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(args);
                return fragment;
            }
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


    /**
     * A async task to execute resources functions (connect, loadChildren, etc.) asynchronously.
     */
    protected class InitAsyncTask
            extends AsyncTask<Void, Integer, Boolean> implements IProgressor
    {
        protected String mMessage;
        protected Account mAccount;
        protected int mMaxProgress;
        protected String mProgressMessage;
        protected int mStep;


        public InitAsyncTask(Account account) {
            mAccount = account;
            mMaxProgress = 0;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            // step 1: connect to server
            mStep = 0;
            int nTimeout = 4000;
            final MainApplication app = (MainApplication) getApplication();
            final String sLogin = app.getAccountLogin(mAccount);
            final String sPassword = app.getAccountPassword(mAccount);
            final String sURL = app.getAccountUrl(mAccount);

            if (null == sURL || null == sPassword || null == sLogin) {
                return false;
            }

            Connection connection = new Connection("tmp", sLogin, sPassword, sURL);
            publishProgress(getString(R.string.connecting), mStep, Constants.STEP_STATE_WORK);

            if(!connection.connect()){
                publishProgress(getString(R.string.error_connect_failed), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else{
                publishProgress(getString(R.string.connected), mStep, Constants.STEP_STATE_WORK);
            }

            if(isCancelled())
                return false;

            // step 1: find keys

            publishProgress(getString(R.string.check_tables_exist), mStep, Constants.STEP_STATE_WORK);

            Map<String, Long> keys = new HashMap<>();
            keys.put(Constants.KEY_CITIZEN_MESSAGES, -1L);

            if(!checkServerLayers(connection, keys)){
                publishProgress(getString(R.string.error_wrong_server), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;

            // step 2: get inspector detail
            // name, description, bbox
            mStep = 1;

            publishProgress(getString(R.string.working), mStep, Constants.STEP_STATE_WORK);

            if(!getInspectorDetail(connection, keys.get(Constants.KEY_INSPECTORS), sLogin)){
                publishProgress(getString(R.string.error_get_inspector_detail), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;

            // step 3: create base layers

            mStep = 2;
            MapBase map = app.getMap();

            createBasicLayers(map, this, mStep);

            if(isCancelled())
                return false;

            // step 4: forest cadastre

            mStep = 3;

            publishProgress(getString(R.string.working), mStep, Constants.STEP_STATE_WORK);

            if (!loadForestCadastre(keys.get(Constants.KEY_CADASTRE), mAccount.name, map, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;

            // step 5: load documents

            mStep = 4;

            publishProgress(getString(R.string.working), mStep, Constants.STEP_STATE_WORK);

            if (!loadDocuments(keys.get(Constants.KEY_DOCUMENTS), mAccount.name, map, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;

            // step 6: load sheets

            mStep = 5;
            int nSubStep = 1;
            int nTotalSubSteps = 7;
            DocumentsLayer documentsLayer = null;

            for(int i = 0; i < map.getLayerCount(); i++){
                ILayer layer = map.getLayer(i);
                if(layer instanceof DocumentsLayer){
                    documentsLayer = (DocumentsLayer) layer;
                }
            }

            publishProgress(getString(R.string.working), mStep, Constants.STEP_STATE_WORK);

            if (!loadLinkedTables(keys.get(Constants.KEY_SHEET), mAccount.name,
                    Constants.KEY_LAYER_SHEET, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }

            if(isCancelled())
                return false;

            // step 6: load productions

            publishProgress(nSubStep + " " + getString(R.string.of) + " " + nTotalSubSteps, mStep,
                    Constants.STEP_STATE_WORK);
            nSubStep++;

            if (!loadLinkedTables(keys.get(Constants.KEY_PRODUCTION), mAccount.name,
                    Constants.KEY_LAYER_PRODUCTION, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }

            if(isCancelled())
                return false;

            // step 6: load vehicles

            publishProgress(nSubStep + " " + getString(R.string.of) + " " + nTotalSubSteps, mStep,
                    Constants.STEP_STATE_WORK);
            nSubStep++;

            if (!loadLinkedTables(keys.get(Constants.KEY_VEHICLES), mAccount.name,
                    Constants.KEY_LAYER_VEHICLES, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }

            if(isCancelled())
                return false;

            publishProgress(nSubStep + " " + getString(R.string.of) + " " + nTotalSubSteps, mStep,
                    Constants.STEP_STATE_WORK);
            nSubStep++;

            if (!loadLookupTables(keys.get(Constants.KEY_VIOLATE_TYPES), mAccount.name,
                    Constants.KEY_LAYER_VIOLATE_TYPES, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }

            if(isCancelled())
                return false;

            publishProgress(nSubStep + " " + getString(R.string.of) + " " + nTotalSubSteps, mStep,
                    Constants.STEP_STATE_WORK);
            nSubStep++;

            if (!loadLookupTables(keys.get(Constants.KEY_SPECIES_TYPES), mAccount.name,
                    Constants.KEY_LAYER_SPECIES_TYPES, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }

            if(isCancelled())
                return false;

            publishProgress(nSubStep + " " + getString(R.string.of) + " " + nTotalSubSteps, mStep,
                    Constants.STEP_STATE_WORK);
            nSubStep++;

            if (!loadLookupTables(keys.get(Constants.KEY_THICKNESS_TYPES), mAccount.name,
                    Constants.KEY_LAYER_THICKNESS_TYPES, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }

            if(isCancelled())
                return false;

            publishProgress(nSubStep + " " + getString(R.string.of) + " " + nTotalSubSteps, mStep,
                    Constants.STEP_STATE_WORK);

            if (!loadLookupTables(keys.get(Constants.KEY_FOREST_CAT_TYPES), mAccount.name,
                    Constants.KEY_LAYER_FOREST_CAT_TYPES, documentsLayer, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            if(isCancelled())
                return false;

            // step 7: load notes

            mStep = 6;

            publishProgress(getString(R.string.working), mStep, Constants.STEP_STATE_WORK);

            if (!loadNotes(keys.get(Constants.KEY_NOTES), mAccount.name, map, this)){
                publishProgress(getString(R.string.error_unexpected), mStep, Constants.STEP_STATE_ERROR);

                try {
                    Thread.sleep(nTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return false;
            }
            else {
                publishProgress(getString(R.string.done), mStep, Constants.STEP_STATE_DONE);
            }

            //TODO: load additional tables

            map.save();

            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            InitStepListAdapter.InitStep step =
                    (InitStepListAdapter.InitStep) mAdapter.getItem(values[0]);
            step.mStepDescription = mMessage;
            step.mState = values[1];

            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(!result){
                //delete map
                final MainApplication app = (MainApplication) getApplication();
                String accName = mAccount.name;
                app.removeAccount(mAccount);

                for(int i = 0; i < 10; i++){
                    if(app.getAccount(accName) == null)
                        break;
                }
            }
            refreshActivityView();
        }

        public final void publishProgress(String message, int step, int state) {
            mMessage = message;
            publishProgress(step, state);

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void setMax(int maxValue) {
            mMaxProgress = maxValue;
        }

        @Override
        public boolean isCanceled() {
            return super.isCancelled();
        }

        @Override
        public void setValue(int value) {
            mMessage = mProgressMessage + " (" + value + " " + getString(R.string.of) + " " + mMaxProgress + ")";
            publishProgress(mStep, Constants.STEP_STATE_WORK);
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {

        }

        @Override
        public void setMessage(String message) {
            mProgressMessage = message;
        }
    }

}
