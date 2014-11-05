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

package fr.tjdev.randcity.vrgame;

import android.os.Bundle;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import fr.tjdev.randcity.R;
import fr.tjdev.randcity.util.OpenGL;

public class VRGameActivity extends CardboardActivity {

    private static final String TAG = "VRGameActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vrgame);
        CardboardView vrView = (CardboardView) findViewById(R.id.vr_view);

        // Check OpenGL ES 2.0 support
        if (OpenGL.hasOpenGLES20Support(this)) {
            vrView.setRenderer(new VRRenderer(this));
            setCardboardView(vrView);
        } else {
            Log.wtf(TAG, getResources().getString(R.string.noOpenGLSupport));
            finish();
            return;
        }
    }

}
