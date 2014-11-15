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
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import fr.tjdev.commonvrlibrary.BluetoothManager;
import fr.tjdev.commonvrlibrary.VROverlayView;
import fr.tjdev.commonvrlibrary.util.OpenGLCheck;
import fr.tjdev.randcity.CommonGLRenderManager;
import fr.tjdev.randcity.MainActivity;
import fr.tjdev.randcity.R;

public class VRGameActivity extends CardboardActivity {

    private static final String TAG = "VRGameActivity";
    private VROverlayView mOverlayView;
    private VRRenderer mRenderer;
    private Vibrator mVibrator;

    private boolean mBluetooth;
    private BluetoothManager mBTManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the bluetooth param
        Bundle bundle = getIntent().getExtras();
        if (bundle.getBoolean(MainActivity.PREF_BT, false)) {
            Log.d(TAG, "Bluetooth support enabled !");
            mBluetooth = true;
            mBTManager = new BluetoothManager(this);
        } else {
            mBluetooth = false;
        }

        setContentView(R.layout.activity_vrgame);
        CardboardView vrView = (CardboardView) findViewById(R.id.vr_view);
        // Set the projection matrix
        vrView.setZPlanes(CommonGLRenderManager.PROJECTION_NEAR, CommonGLRenderManager.PROJECTION_FAR);
        mOverlayView = (VROverlayView) findViewById(R.id.vr_overlay);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Check OpenGL ES 2.0 support
        if (OpenGLCheck.hasOpenGLES20Support(this)) {
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
                mOverlayView.show3DToast(getString(R.string.fogOff));
                mRenderer.enableFog = false;
            } else {
                mOverlayView.show3DToast(getString(R.string.fogOn));
                mRenderer.enableFog = true;
            }
            mVibrator.vibrate(50);
        }
    }

    // Pass the results info to the Bluetooth Manager.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBluetooth) {
            if (!mBTManager.onActivityResult(requestCode, resultCode)) {
               mBluetooth = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetooth) {
            mBTManager.onDestroy();
        }
    }
}
