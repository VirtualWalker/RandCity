/*
 * This file is part of RandCity.
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

package fr.tjdev.randcity;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.tjdev.randcity.testgame.TestGameActivity;
import fr.tjdev.randcity.vrgame.VRGameActivity;

/**
 * This activity allow users to choose the game mode : normal mode or VR mode
 */
public class MainActivity extends ListActivity {
    private static final String ITEM_IMAGE = "img";
    private static final String ITEM_TITLE = "title";
    private static final String ITEM_SUBTITLE = "subtitle";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.headerTitle);
        setContentView(R.layout.activity_main);

        // Initialize data
        final List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        final SparseArray<Class<? extends Activity>> activityMapping = new SparseArray<Class<? extends Activity>>();

        int i = 0;

        {
            final Map<String, Object> item = new HashMap<String, Object>();
            item.put(ITEM_IMAGE, R.drawable.test_game_icon);
            item.put(ITEM_TITLE, getText(R.string.testGameLabel));
            item.put(ITEM_SUBTITLE, getText(R.string.testGameSubtitle));
            data.add(item);
            activityMapping.put(i++, TestGameActivity.class);
        }
        {
            final Map<String, Object> item = new HashMap<String, Object>();
            item.put(ITEM_IMAGE, R.drawable.icon);
            item.put(ITEM_TITLE, getText(R.string.vrGameLabel));
            item.put(ITEM_SUBTITLE, getText(R.string.vrGameSubtitle));
            data.add(item);
            activityMapping.put(i, VRGameActivity.class);
        }

        final SimpleAdapter dataAdapter = new SimpleAdapter(this, data, R.layout.toc_item, new String[]{ITEM_IMAGE, ITEM_TITLE, ITEM_SUBTITLE}, new int[]{R.id.tocImage, R.id.tocTitle, R.id.tocSubTitle});
        setListAdapter(dataAdapter);

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                final Class<? extends Activity> activityToLaunch = activityMapping.get(position);

                if (activityToLaunch != null) {
                    final Intent launchIntent = new Intent(MainActivity.this, activityToLaunch);
                    startActivity(launchIntent);
                }
            }
        });
    }
}
