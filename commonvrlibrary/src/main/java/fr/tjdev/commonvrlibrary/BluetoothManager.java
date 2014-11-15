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

package fr.tjdev.commonvrlibrary;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import fr.tjdev.commonvrlibrary.util.RawResourceReader;

/**
 * Manage bluetooth connections to communicate with an another system
 * and receive positions data. The class is designed to be client-side.
 *
 * You must call the constructor in the Activity's onCreate().
 */
public class BluetoothManager {
    private static final String TAG = "BluetoothManager";

    // The activity that the manager depends on
    private Activity mParentActivity;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int HANDLER_MESSAGE_READ = 2;

    // Used to get the bluetooth support
    private BluetoothAdapter mBluetoothAdapter;

    private final boolean mResetOnDestroy;

    // Contains devices (MAC addresses) that the phone can connect to.
    // This list is created in the constructor by reading the raw res file
    // "allowed_bt_servers.txt"
    // Order in the list represent the priority, the first will be choose first.
    private ArrayList<String> mAllowedServers = new ArrayList<String>();

    // Contains all devices found.
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();

    private ConnectThread mConnectThread = null;
    private ConnectedThread mConnectedThread = null;

    private static final UUID mUUID = UUID.fromString("c1b549ed-71a1-4b9b-870c-c48de880553e");

    private boolean mFirstScan = true;

    // Static methods used to disable/enable the bluetooth (don't use it without an user prompt)
    public static void disableBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.disable();
        }
    }
    public static void enableBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.enable();
        }
    }

    // Broadcast receiver
    // Used to listen to these actions :
    // - ACTION_FOUND : when a new device is found
    // - ACTION_DISCOVERY_FINISHED : when the discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the device if is not already in the list
                if (!mDevices.contains(device)) {
                    mDevices.add(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mFirstScan) {
                    Log.d(TAG, "Discovery finished");

                    // Print devices addresses
                    Log.d(TAG, "Near devices: ");
                    for (BluetoothDevice device : mDevices) {
                        Log.d(TAG, device.getAddress() + " (" + device.getName() + ")");
                    }

                    // The scan is finished, check if a device is in the mAllowedServers list.
                    for (String allowed : mAllowedServers) {
                        for (BluetoothDevice btDevice : mDevices) {
                            if (btDevice.getAddress().equals(allowed)) {
                                // A device were found
                                Log.d(TAG, "Use device: " + btDevice.getAddress());

                                // Clean up previous connected devices
                                if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
                                if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

                                mConnectThread = new ConnectThread(btDevice);
                                mConnectThread.run();

                                // Exit the loop
                                mFirstScan = false;
                                return;
                            }
                        }
                    }

                    // Here, no devices were found
                    Log.w(TAG, "No bluetooth servers were found ...");
                }
                if (mFirstScan) {
                    mFirstScan = false;
                }
            }
        }
    };

    // Handler to receive read data
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what != HANDLER_MESSAGE_READ) {
                return false;
            }
            // At this stage, just print the received value
            Log.d(TAG, "Received string :");
            byte[] bts = ((byte[]) msg.obj);
            Log.d(TAG, new String(bts));
            return true;
        }
    });

    public BluetoothManager(Activity activity) {
        this(activity, false);
    }

    public BluetoothManager(Activity activity, boolean resetOnDestroy) {
        mParentActivity = activity;
        mResetOnDestroy = resetOnDestroy;

        // Check Bluetooth support
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.wtf(TAG, "No bluetooth support on this device !");
            mParentActivity.finish();
            return;
        }
        Log.d(TAG, "This device supports Bluetooth.");

        // Get a list of allowed servers
        getAllowedServers();

        // Register broadcast receivers
        // They are unregistered in the onDestroy();
        IntentFilter filterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mParentActivity.registerReceiver(mReceiver, filterFound);
        IntentFilter filterFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mParentActivity.registerReceiver(mReceiver, filterFinished);

        // Enable bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mParentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.d(TAG, "Enabling Bluetooth ...");
        } else {
            // The bluetooth is already on.
            Log.d(TAG, "Bluetooth is already enabled.");
            searchForDevices();
        }
    }

    // Used to check if the bluetooth is enabled
    public boolean onActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_ENABLE_BT) {
           if (resultCode == Activity.RESULT_OK) {
               Log.d(TAG, "Successfully enabling Bluetooth.");
               searchForDevices();
               return true;
           } else {
               Log.e(TAG, "Error when enabling Bluetooth !");
               return false;
           }
        }
        return true;
    }

    // Called in the onDestroy() method of the parent activity
    public void onDestroy() {
        mBluetoothAdapter.cancelDiscovery();
        mParentActivity.unregisterReceiver(mReceiver);

        // Reset the bluetooth if needed
        if (mResetOnDestroy) {
            mBluetoothAdapter.disable();
        }
    }

    private void searchForDevices() {
        mDevices.clear();

        //
        // Querying paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the address
                mDevices.add(device);
            }
        }

        //
        // Search for new devices
        // The end of this function will be executed in the mScanFinishedReceiver
        mBluetoothAdapter.startDiscovery();
    }

    private void getAllowedServers() {
        final String txtFile = RawResourceReader.readTextFileFromRawResource(mParentActivity, R.raw.allowed_bt_servers);

        final List<String> list = Arrays.asList(txtFile.split("\n"));
        mAllowedServers.clear();

        // Remove comments and spaces
        for (String line : list) {
            line = line.replaceAll("\\s+", "");
            line = line.toUpperCase(Locale.US);
            // Check that is not a comment and it's a MAC address
            if (!line.startsWith("#") && BluetoothAdapter.checkBluetoothAddress(line)) {
                mAllowedServers.add(line);
            }
        }

        Log.d(TAG, "Allowed Bluetooth servers: ");
        for (String serv : mAllowedServers) {
            Log.d(TAG, serv);
        }
    }

    // Create the ConnectedThread
    private void manageConnectedSocket(BluetoothSocket socket) {
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.run();
    }

    /**
     * Used to connect with a specified device.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mSocket,
            // because mSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e) {
                Log.e(TAG, "Exception:", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Exception:", closeException);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothManager.this) {
                mConnectThread = null;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception:", e);
            }
        }
    }

    /**
     * Used to read/write data with a connected device
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Exception:", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(HANDLER_MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Exception:", e);
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Exception:", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception:", e);
            }
        }
    }
}
