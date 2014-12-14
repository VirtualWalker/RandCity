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
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import fr.tjdev.commonvrlibrary.util.FileHelper;
import fr.tjdev.commonvrlibrary.util.RawResourceReader;

/**
 * Manage bluetooth connections to communicate with an another system
 * and receive positions data. The class is designed to be client-side.
 *
 * You must call the constructor in the Activity's onCreate().
 */
public class BluetoothManager {
    private static final String TAG = "BluetoothManager";

    // Names of files in the external storage
    private static final String ALLOWED_SERVER_FILENAME = "allowed_bt_servers.txt";

    // The activity that the manager depends on
    private final Activity mParentActivity;

    // Custom handlers
    private static final int REQUEST_ENABLE_BT = 1;

    // Custom Broadcasts (associated strings)
    // The activity can create a BroadcastReceiver to listen to these actions
    public static final String ACTION_NO_SERVERS_FOUND = "fr.tjdev.commonvrlibrary.bluetooth.action.NO_SERVERS_FOUND";
    public static final String ACTION_CONNECT_FAILED = "fr.tjdev.commonvrlibrary.bluetooth.action.CONNECT_FAILED";
    public static final String ACTION_CONNECT_SUCCESS = "fr.tjdev.commonvrlibrary.bluetooth.action.CONNECT_SUCCESS";
    public static final String ACTION_BT_ENABLED = "fr.tjdev.commonvrlibrary.bluetooth.action.BT_ENABLED";
    public static final String ACTION_BT_NOT_ENABLED = "fr.tjdev.commonvrlibrary.bluetooth.action.BT_NOT_ENABLED";
    public static final String ACTION_SEARCH_START = "fr.tjdev.commonvrlibrary.bluetooth.action.SEARCH_START";
    public static final String ACTION_SEARCH_END = "fr.tjdev.commonvrlibrary.bluetooth.action.SEARCH_END";

    // Used to get the bluetooth support
    private final BluetoothAdapter mBluetoothAdapter;

    private final boolean mResetOnDestroy;

    // Contains devices (MAC addresses) that the phone can connect to.
    // This list is created in the constructor by reading the raw res file
    // "allowed_bt_servers.txt"
    // Order in the list represent the priority, the first will be choose first.
    private ArrayList<String> mAllowedServers = new ArrayList<>();

    // Contains all devices found.
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();

    // Contains the address of the connected device
    private String mDeviceAddress;

    private ConnectThread mConnectThread = null;
    private ConnectedThread mConnectedThread = null;

    // This UUID represent the server.
    // Don't modify this since it's also hard-coded in server
    // It's allow to connect to the server without a specified channel
    private static final UUID mUUID = UUID.fromString("23010000-6745-0000-AB89-0000EFCD0000");

    private boolean mFirstScan = true;

    // Static methods used to disable/enable the bluetooth (don't use it without an user prompt)
    public static void disableBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Log.d(TAG, "Disabling bluetooth.");
            adapter.disable();
        }
    }
    public static void enableBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Log.d(TAG, "Enabling bluetooth.");
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
                    mParentActivity.sendBroadcast(new Intent(ACTION_SEARCH_END));

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
                                mDeviceAddress = btDevice.getAddress();
                                Log.d(TAG, "Use device: " + mDeviceAddress);

                                // Clean up previous connected devices
                                if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
                                if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

                                mConnectThread = new ConnectThread(btDevice);
                                mConnectThread.start();

                                // Exit the loop
                                mFirstScan = false;
                                return;
                            }
                        }
                    }

                    // Here, no devices were found
                    Log.w(TAG, "No bluetooth servers were found ...");
                    mParentActivity.sendBroadcast(new Intent(ACTION_NO_SERVERS_FOUND));
                    // Disable bluetooth
                    mBluetoothAdapter.disable();
                }
                if (mFirstScan) {
                    mFirstScan = false;
                }
            }
        }
    };

    // Custom listener used when data are received with bluetooth
    public interface OnBluetoothDataListener {
        void onNewData(final int walkSpeed, final int orientation);
    }

    private OnBluetoothDataListener mOnBTDataListener;

    // Constructors
    public BluetoothManager(Activity activity) {
        this(activity, false);
    }

    public BluetoothManager(Activity activity, boolean resetOnDestroy) {
        mParentActivity = activity;
        mResetOnDestroy = resetOnDestroy;

        // Copy some resources files if needed
        if (!FileHelper.hasExternalStoragePrivateFile(mParentActivity, ALLOWED_SERVER_FILENAME)) {
            FileHelper.createExternalStoragePrivateFile(mParentActivity, ALLOWED_SERVER_FILENAME, R.raw.allowed_bt_servers);
        }

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
               mParentActivity.sendBroadcast(new Intent(ACTION_BT_ENABLED));
               searchForDevices();
               return true;
           } else {
               Log.e(TAG, "Error when enabling Bluetooth !");
               mParentActivity.sendBroadcast(new Intent(ACTION_BT_NOT_ENABLED));
               return false;
           }
        }
        return true;
    }

    // Call in the onDestroy() method of the parent activity
    public void onDestroy() {
        mBluetoothAdapter.cancelDiscovery();
        mParentActivity.unregisterReceiver(mReceiver);

        // Reset the bluetooth if needed
        if (mResetOnDestroy) {
            mBluetoothAdapter.disable();
        }
    }

    public void setOnBluetoothDataListener(OnBluetoothDataListener listener) {
        mOnBTDataListener = listener;
    }

    private void searchForDevices() {
        mDevices.clear();
        // Search for new devices
        // The end of this function will be executed in the mScanFinishedReceiver
        mParentActivity.sendBroadcast(new Intent(ACTION_SEARCH_START));
        Log.d(TAG, "Starting discovery ...");
        mBluetoothAdapter.startDiscovery();
    }

    // The list of allowed servers is read from the external storage first, and from the
    // app-resource if doesn't exists
    private void getAllowedServers() {
        String txtFile;
        boolean hasExternalStorage = FileHelper.hasExternalStoragePrivateFile(mParentActivity, ALLOWED_SERVER_FILENAME);
        if (hasExternalStorage) {
            txtFile = FileHelper.readExternalStoragePrivateFile(mParentActivity, ALLOWED_SERVER_FILENAME);
        } else {
            // Read the file from the resources, since there is no external storage
            txtFile = RawResourceReader.readTextFileFromRawResource(mParentActivity, R.raw.allowed_bt_servers);
        }

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
        for (String server : mAllowedServers) {
            Log.d(TAG, server);
        }
    }

    // Create the ConnectedThread
    private void manageConnectedSocket(BluetoothSocket socket) {
        shutdownConnection();

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    public void shutdownConnection() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel(); mConnectedThread = null;
        }
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
                tmp = device.createInsecureRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread exception:", e);
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
                // Unable to connect, close the socket and get out
                Log.e(TAG, "ConnectThread run exception:", connectException);
                // Send an error broadcast
                mParentActivity.sendBroadcast(new Intent(ACTION_CONNECT_FAILED));

                try {
                    mmSocket.close();
                    mBluetoothAdapter.disable();
                } catch (IOException closeException) {
                    Log.e(TAG, "ConnectThread run exception 2:", closeException);
                }
                return;
            }

            // Here, we are connected
            mParentActivity.sendBroadcast(new Intent(ACTION_CONNECT_SUCCESS));
            Log.d(TAG, "Connected to device: " + mDeviceAddress);

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread cancel exception:", e);
            }
        }
    }

    /**
     * Used to read/write data with a connected device
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread exception:", e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            // Store 3 numbers
            byte[] buffer = new byte[3];
            int bytesCount; // bytes returned from read()

            // Used to show less debug output
            final int printFrequency = 10;
            int dataCount = 0;

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytesCount = mmInStream.read(buffer);
                    if (bytesCount == 3) {
                        // Check if the first byte is 0xFF
                        final int messageCheck = buffer[0] & 0xFF;
                        if (messageCheck == 0xFF) {
                            // Get the speed and the orientation
                            final int walkSpeed = buffer[1] & 0xFF;
                            final int orientation = buffer[2] & 0xFF;
                            final int realOrientation = (int) (orientation * (360.0f/255.0f));

                            dataCount++;
                            // Show debug output only every second
                            if (dataCount == printFrequency) {
                                dataCount = 0;
                                // Don't show the message if received data are null
                                if (walkSpeed != 0 || orientation != 0) {
                                    Log.v(TAG, "Receive data: speed=" + Integer.toString(walkSpeed)
                                            + " orientation=" + Integer.toString(orientation)
                                            + " (real orientation: " + Integer.toString(realOrientation) + ")");
                                }
                            }

                            // Send data to the listener
                            if (mOnBTDataListener != null) {
                                mOnBTDataListener.onNewData(walkSpeed, realOrientation);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread run exception:", e);
                    try {
                        mmSocket.close();
                        mBluetoothAdapter.disable();
                    } catch (IOException closeException) {
                        Log.e(TAG, "ConnectedThread run exception 2:", closeException);
                    }
                    break;
                }
            }
        }

        // Call this to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread cancel exception:", e);
            }
        }
    }
}
