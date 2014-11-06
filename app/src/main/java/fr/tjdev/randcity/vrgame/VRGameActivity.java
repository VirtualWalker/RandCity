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

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import fr.tjdev.randcity.BaseGLRenderer;
import fr.tjdev.randcity.R;
import fr.tjdev.randcity.util.OpenGL;

public class VRGameActivity extends CardboardActivity {

    private static final String TAG = "VRGameActivity";
    private VROverlayView mOverlayView;
    private VRRenderer mRenderer;
    private Vibrator mVibrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vrgame);
        CardboardView vrView = (CardboardView) findViewById(R.id.vr_view);
        vrView.setZPlanes(BaseGLRenderer.PROJECTION_NEAR, BaseGLRenderer.PROJECTION_FAR);
        mOverlayView = (VROverlayView) findViewById(R.id.vr_overlay);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Check OpenGL ES 2.0 support
        if (OpenGL.hasOpenGLES20Support(this)) {
            mRenderer = new VRRenderer(this);
            vrView.setRenderer(mRenderer);
            setCardboardView(vrView);
        } else {
            Log.wtf(TAG, getString(R.string.noOpenGLSupport));
            finish();
            return;
        }

        vrView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFog();
            }
        });
    }

    @Override
    public void onCardboardTrigger() {
        toggleFog();
    }

    // Enable/Disable the fog on trigger the magnet or on touch the screen
    public void toggleFog() {
        if (mOverlayView != null && mRenderer != null) {
            if (mRenderer.enableFog) {
                mOverlayView.show3DToast("Disable fog");
                mRenderer.enableFog = false;
            } else {
                mOverlayView.show3DToast("Enable fog");
                mRenderer.enableFog = true;
            }
            mVibrator.vibrate(50);
        }
    }
}
