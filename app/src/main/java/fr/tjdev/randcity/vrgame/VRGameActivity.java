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
import android.os.Handler;
import android.view.View;

import fr.tjdev.commonvrlibrary.BluetoothManager;
import fr.tjdev.commonvrlibrary.activities.VRActivity;
import fr.tjdev.randcity.CommonGLRenderManager;
import fr.tjdev.randcity.R;

public class VRGameActivity extends VRActivity {
    private VRRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the projection matrix
        mVrView.setZPlanes(CommonGLRenderManager.PROJECTION_NEAR, CommonGLRenderManager.PROJECTION_FAR);

        mRenderer = new VRRenderer(this);
        enableRenderer(mRenderer);

        mRenderer.setOnTreasureFoundListener(new CommonGLRenderManager.OnTreasureFoundListener() {
            @Override
            public void onTreasureFound() {
                mOverlayView.show3DToast(getString(R.string.treasureFound));

                // Schedule the exit of the game
                Handler scheduler = new Handler();
                scheduler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1500);
            }
        });

        // Toggle fog on click
        mVrView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFog();
            }
        });

        // Set the bluetooth listener
        if (mBluetooth) {
            mBTManager.setOnBluetoothDataListener(new BluetoothManager.OnBluetoothDataListener() {
                @Override
                public void onNewData(int walkSpeed, int orientation) {
                    final float move = walkSpeed / 80.0f;
                    final float moveZ = (float) (Math.cos(Math.toRadians(orientation)) * move);
                    final float moveX = (float) (Math.sin(Math.toRadians(orientation)) * move);

                    mRenderer.movePlayer(moveX, moveZ);
                }
            });
        }
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
