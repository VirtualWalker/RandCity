/*
 * Copyright (c) 2014 Fabien Caylus <toutjuste13@gmail.com>
 *
 * This file is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.tjdev.commonvrlibrary.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.tjdev.commonvrlibrary.FullScreenManager;
import fr.tjdev.commonvrlibrary.R;

/**
 * This class is a list activity that contains all games modes.
 * The desired activities must be added in the onCreate() method because the final layout is created in onStart().
 */
public class MainMenuActivity extends ListActivity {
    private static final String ITEM_IMAGE = "img";
    private static final String ITEM_TITLE = "title";
    private static final String ITEM_SUBTITLE = "subtitle";

    public static final String PREF_BT = "bt";
    public static final String PREF_BT_RESET = "bt_reset";

    protected boolean mBluetoothEnabled;
    protected boolean mBluetoothReset;

    private FullScreenManager mFullScreenMgr = new FullScreenManager(this);

    // Initialize the list of activities
    private List<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    private List<Class<? extends Activity>> mActivityMapping = new ArrayList<Class<? extends Activity>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFullScreenMgr.onCreate();

        // Set the default layout (with games modes and bluetooth checkboxes)
        setContentView(R.layout.activity_mainmenu);

        // Get the preferences
        mBluetoothEnabled = getPreferences(MODE_PRIVATE).getBoolean(PREF_BT, false);
        if (mBluetoothEnabled) {
            ((CheckBox) findViewById(R.id.checkBoxBluetooth)).setChecked(true);
        }
        mBluetoothReset = getPreferences(MODE_PRIVATE).getBoolean(PREF_BT_RESET, false);
        if (mBluetoothReset) {
            ((CheckBox) findViewById(R.id.checkBoxBluetoothReset)).setChecked(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Create the adapter for the list view
        final SimpleAdapter dataAdapter = new SimpleAdapter(this, mData, R.layout.toc_item, new String[]{ITEM_IMAGE, ITEM_TITLE, ITEM_SUBTITLE}, new int[]{R.id.tocImage, R.id.tocTitle, R.id.tocSubTitle});
        setListAdapter(dataAdapter);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                final Class<? extends Activity> activityToLaunch = mActivityMapping.get(position);

                if (activityToLaunch != null) {
                    final Intent launchIntent = new Intent(MainMenuActivity.this, activityToLaunch);
                    // Check the bluetooth support
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(PREF_BT, mBluetoothEnabled);
                    bundle.putBoolean(PREF_BT_RESET, mBluetoothReset);
                    launchIntent.putExtras(bundle);

                    startActivity(launchIntent);
                }
            }
        });
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Save preferences
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putBoolean(PREF_BT, mBluetoothEnabled);
        editor.putBoolean(PREF_BT_RESET, mBluetoothReset);
        editor.apply();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mFullScreenMgr.onWindowFocusChanged(hasFocus);
    }

    // Listen to the check box click
    public void onBTCheckBoxClicked(View view) {
        mBluetoothEnabled = ((CheckBox) view).isChecked();
    }
    public void onBTResetCheckBoxClicked(View view) {
        mBluetoothReset = ((CheckBox) view).isChecked();
    }

    // Allow the child activity to add an item to the main menu
    protected void addItem(int icon, int label, int subTitle, Class<? extends Activity> activity) {
        final Map<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_IMAGE, icon);
        item.put(ITEM_TITLE, getText(label));
        item.put(ITEM_SUBTITLE, getText(subTitle));
        mData.add(item);
        mActivityMapping.add(activity);
    }

    // Allow to change the header text
    protected void setHeaderText(int text) {
        ((TextView) findViewById(R.id.mainMenuHeader)).setText(text);
    }
    protected void setHeaderText(String text) {
        ((TextView) findViewById(R.id.mainMenuHeader)).setText(text);
    }
}
