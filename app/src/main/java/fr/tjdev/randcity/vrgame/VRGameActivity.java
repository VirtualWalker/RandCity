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
import android.view.View;

import fr.tjdev.commonvrlibrary.activities.VRActivity;
import fr.tjdev.commonvrlibrary.util.OpenGLCheck;
import fr.tjdev.randcity.CommonGLRenderManager;
import fr.tjdev.randcity.R;

public class VRGameActivity extends VRActivity {

    private static final String TAG = "VRGameActivity";
    private VRRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the projection matrix
        mVrView.setZPlanes(CommonGLRenderManager.PROJECTION_NEAR, CommonGLRenderManager.PROJECTION_FAR);

        // Check OpenGL ES 2.0 support
        if (OpenGLCheck.hasOpenGLES20Support(this)) {
            mRenderer = new VRRenderer(this);
            mVrView.setRenderer(mRenderer);
        } else {
            Log.wtf(TAG, getString(R.string.noOpenGLSupport));
            finish();
            return;
        }

        mVrView.setOnClickListener(new View.OnClickListener() {
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
                mOverlayView.show3DToast(getString(R.string.fogOff));
                mRenderer.enableFog = false;
            } else {
                mOverlayView.show3DToast(getString(R.string.fogOn));
                mRenderer.enableFog = true;
            }
            mVibrator.vibrate(50);
        }
    }
}
