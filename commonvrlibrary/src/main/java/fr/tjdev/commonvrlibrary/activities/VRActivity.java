/*
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

package fr.tjdev.commonvrlibrary.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import fr.tjdev.commonvrlibrary.BluetoothManager;
import fr.tjdev.commonvrlibrary.R;
import fr.tjdev.commonvrlibrary.VROverlayView;
import fr.tjdev.commonvrlibrary.util.OpenGLCheck;

public abstract class VRActivity extends CardboardActivity {
    private static final String TAG = "VRActivity";

    protected VROverlayView mOverlayView;
    protected CardboardView mVrView;
    protected Vibrator mVibrator;

    protected boolean mBluetooth;
    protected BluetoothManager mBTManager;

    protected boolean mDebugRenderer;

    protected final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case BluetoothManager.ACTION_BT_NOT_ENABLED:
                    mOverlayView.showError3DToast(getString(R.string.bluetoothNotEnabled));
                    break;
                case BluetoothManager.ACTION_CONNECT_FAILED:
                    mOverlayView.showError3DToast(getString(R.string.bluetoothConnectFailed));
                    break;
                case BluetoothManager.ACTION_CONNECT_SUCCESS:
                    mOverlayView.show3DToast(getString(R.string.bluetoothConnectSuccess));
                    break;
                case BluetoothManager.ACTION_NO_SERVERS_FOUND:
                    mOverlayView.showError3DToast(getString(R.string.bluetoothNoServersFound));
                    break;
                case BluetoothManager.ACTION_SEARCH_START:
                    mOverlayView.show3DToast(getString(R.string.bluetoothSearchStart));
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the bluetooth param
        Bundle bundle = getIntent().getExtras();
        if (bundle.getBoolean(MainMenuActivity.PREF_BT, false)) {
            Log.d(TAG, "Bluetooth support enabled !");
            mBluetooth = true;
            mBTManager = new BluetoothManager(this);

            // Register broadcast receivers for bluetooth events
            // They are unregistered in the onDestroy();
            registerReceiver(mReceiver, new IntentFilter(BluetoothManager.ACTION_BT_NOT_ENABLED));
            registerReceiver(mReceiver, new IntentFilter(BluetoothManager.ACTION_CONNECT_FAILED));
            registerReceiver(mReceiver, new IntentFilter(BluetoothManager.ACTION_CONNECT_SUCCESS));
            registerReceiver(mReceiver, new IntentFilter(BluetoothManager.ACTION_NO_SERVERS_FOUND));
            registerReceiver(mReceiver, new IntentFilter(BluetoothManager.ACTION_SEARCH_START));
        } else {
            mBluetooth = false;
        }

        setContentView(R.layout.activity_vr);
        // The projection matrix for this view must be set in the child class
        mVrView = (CardboardView) findViewById(R.id.vr_view);
        setCardboardView(mVrView);
        mOverlayView = (VROverlayView) findViewById(R.id.vr_overlay);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mDebugRenderer = bundle.getBoolean(MainMenuActivity.PREF_DEBUG_ACT, false);
        // Disable VR mode on debug
        if(mDebugRenderer) {
            mVrView.setVRModeEnabled(false);
        } else {
            // On not-debug mode, we need to hide the button
            findViewById(R.id.button_move_forward).setVisibility(View.GONE);
        }

        // You need to set the renderer on your own in the child class
        // and call enableRenderer() to enable it
    }

    // Must be call only once in the onCreate() method of the child
    protected void enableRenderer(CardboardView.StereoRenderer stereoRenderer) {
        // Check OpenGL ES 2.0 support
        if (OpenGLCheck.hasOpenGLES20Support(this)) {
            if (stereoRenderer != null) {
                mVrView.setRenderer(stereoRenderer);
            }
        } else {
            Log.wtf(TAG, getString(R.string.noOpenGLSupport));
            finish();
        }
    }

    // Pass the results info to the Bluetooth Manager.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBluetooth) {
            mBTManager.onActivityResult(requestCode, resultCode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetooth) {
            unregisterReceiver(mReceiver);
            mBTManager.onDestroy();
        }
    }
}
