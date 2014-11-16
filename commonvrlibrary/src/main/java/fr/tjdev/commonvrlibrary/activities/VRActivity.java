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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;

import fr.tjdev.commonvrlibrary.BluetoothManager;
import fr.tjdev.commonvrlibrary.R;
import fr.tjdev.commonvrlibrary.VROverlayView;

public class VRActivity extends CardboardActivity {
    private static final String TAG = "VRActivity";

    protected VROverlayView mOverlayView;
    protected CardboardView mVrView;
    protected Vibrator mVibrator;

    protected boolean mBluetooth;
    protected BluetoothManager mBTManager;

    protected final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothManager.ACTION_BT_NOT_ENABLED.equals(action)) {
                mOverlayView.showError3DToast(getString(R.string.bluetoothNotEnabled));
            } else if (BluetoothManager.ACTION_CONNECT_FAILED.equals(action)) {
                mOverlayView.showError3DToast(getString(R.string.bluetoothConnectFailed));
            } else if (BluetoothManager.ACTION_CONNECT_SUCCESS.equals(action)) {
                mOverlayView.show3DToast(getString(R.string.bluetoothConnectSuccess));
            } else if (BluetoothManager.ACTION_NO_SERVERS_FOUND.equals(action)) {
                mOverlayView.showError3DToast(getString(R.string.bluetoothNoServersFound));
            } else if (BluetoothManager.ACTION_SEARCH_START.equals(action)) {
                mOverlayView.show3DToast(getString(R.string.bluetoothSearchStart));
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

        // You need to set the renderer on your own in the child class
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
