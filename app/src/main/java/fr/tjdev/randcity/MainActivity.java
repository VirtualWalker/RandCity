/*
 * This file is part of RandCity.
 * Copyright (c) 2015 Fabien Caylus <toutjuste13@gmail.com>
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

import android.os.Bundle;

import fr.tjdev.commonvrlibrary.activities.MainMenuActivity;
import fr.tjdev.randcity.testgame.TestGameActivity;
import fr.tjdev.randcity.vrgame.VRGameActivity;

/**
 * This activity allow users to choose the game mode : normal mode or VR mode
 */
public class MainActivity extends MainMenuActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add items to the menu
        super.addItem(R.drawable.test_game_icon, R.string.testGameLabel, R.string.testGameSubtitle, TestGameActivity.class);
        super.addItem(R.drawable.icon, R.string.vrGameLabel, R.string.vrGameSubtitle, VRGameActivity.class);

        // Set the header text
        super.setHeaderText(R.string.header);
    }
}
