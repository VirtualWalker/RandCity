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

package fr.tjdev.randcity.vrgame;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import fr.tjdev.commonvrlibrary.BluetoothManager;
import fr.tjdev.commonvrlibrary.activities.VRActivity;
import fr.tjdev.randcity.BuildConfig;
import fr.tjdev.randcity.R;

public class VRGameActivity extends VRActivity {
    private static final String TAG = "VRGameActivity";

    private VRRenderer mRenderer;

    private int mNumberOfTreasureListenerCall = 0;

    // Used to initialize the angle
    private int mAngleDiff = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRenderer = new VRRenderer(this, mDebugRenderer);
        enableRenderer(mRenderer);

        mRenderer.setOnTreasureFoundListener(new VRRenderer.OnTreasureFoundListener() {
            @Override
            public void onTreasureFound() {
                mNumberOfTreasureListenerCall++;
                if (mNumberOfTreasureListenerCall == 1) {
                    if (mBluetooth) {
                        mBTManager.shutdownConnection();
                    }

                    VRGameActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlayView.show3DToast(getString(R.string.treasureFound));

                            // Schedule the exit of the game
                            Handler scheduler = new Handler();
                            scheduler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!mDebugRenderer) {
                                        finish();
                                    }
                                }
                            }, 4000);
                        }
                    });
                }
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

                    //
                    // Special values for the walk speed:
                    //  - 254 : init with the current orientation
                    //  - 253 : start the game
                    //

                    if (walkSpeed == 254) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Initialization with the phone orientation !");
                        }
                        // Compute the phone orientation
                        mAngleDiff = (int)Math.toDegrees(Math.acos(mRenderer.lookForwardVector[0]));

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Angle diff is :" + Integer.toString(mAngleDiff));
                        }
                        return;
                    } else if (walkSpeed == 253) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Start the game !");
                        }
                        mOverlayView.show3DToast(getString(R.string.startGame));
                        return;
                    }

                    final int realOrientation = orientation + mAngleDiff;

                    final float move = walkSpeed / 80.0f;
                    final float moveZ = (float) (Math.cos(Math.toRadians(realOrientation)) * move);
                    final float moveX = (float) (Math.sin(Math.toRadians(realOrientation)) * move);

                    mRenderer.movePlayer(-moveX, 0.0f, -moveZ);
                }
            });
        }

        // On debug mode, create a click listener for the button
        if(mDebugRenderer) {
            findViewById(R.id.button_move_forward).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final float move = -5.0f;
                    mRenderer.movePlayer(move * mRenderer.lookForwardVector[0], 0.0f, move * mRenderer.lookForwardVector[2]);
                }
            });

            // Disable fog on debug generations
            mRenderer.enableFog = false;
        }
    }

    @Override
    public void onCardboardTrigger() {
        toggleFog();
    }

    // Enable/Disable the fog on magnet trigger or on screen touch
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
