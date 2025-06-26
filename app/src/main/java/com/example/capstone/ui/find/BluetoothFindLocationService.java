package com.example.capstone.ui.find;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.UUID;

public class BluetoothFindLocationService {
    // Debugging
    private static final String TAG = "BluetoothFindLocationService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;
    private final Context mContext;

    // ConstantsForFindLocation that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public BluetoothFindLocationService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
        mContext = context;
    }

    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;
        mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
        updateUserInterfaceTitle();
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        updateUserInterfaceTitle();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bundle.putString(ConstantsForFindLocation.DEVICE_NAME, device.getName());
        } else {
            Toast.makeText(mContext, "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show();
        }
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        updateUserInterfaceTitle();

        // Start BLE scanning
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            mAdapter.startLeScan(mLeScanCallback);
        } else {
            Toast.makeText(mContext, "Bluetooth scanning permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        mState = STATE_NONE;
        updateUserInterfaceTitle();

        // Stop BLE scanning
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            mAdapter.stopLeScan(mLeScanCallback);
        } else {
            Toast.makeText(mContext, "Bluetooth scanning permission not granted", Toast.LENGTH_SHORT).show();
        }

        // Notify the handler to reset the UI
        mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_STATE_CHANGE, STATE_NONE, -1).sendToTarget();
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ConstantsForFindLocation.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        updateUserInterfaceTitle();
        BluetoothFindLocationService.this.start();
    }

    private void connectionLost() {
        Message msg = mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ConstantsForFindLocation.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        updateUserInterfaceTitle();
        BluetoothFindLocationService.this.start();
    }

    public BluetoothDevice getConnectedDevice() {
        if (mConnectedThread != null) {
            return mConnectedThread.getDevice();
        }
        return null;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if (secure) {
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                    } else {
                        tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                    }
                } else {
                    Toast.makeText(mContext, "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + " listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            BluetoothSocket socket;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + " accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothFindLocationService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + " close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if (secure) {
                        tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                    } else {
                        tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                    }
                } else {
                    Toast.makeText(mContext, "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + " create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                mAdapter.cancelDiscovery();
            } else {
                Toast.makeText(mContext, "Bluetooth scanning permission not granted", Toast.LENGTH_SHORT).show();
            }

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothFindLocationService.this) {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
            closeResources();
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            closeResources();
        }

        private void closeResources() {
            try {
                if (mmInStream != null) {
                    mmInStream.close();
                }
                if (mmOutStream != null) {
                    mmOutStream.close();
                }
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        public BluetoothDevice getDevice() {
            return mmSocket.getRemoteDevice();
        }
    }

    private static final int AVERAGE_WINDOW_SIZE = 5;
    private final LinkedList<Integer> rssiValues = new LinkedList<>();

    private int calculateSmoothedSignalStrength(int rssi) {
        if (rssiValues.size() >= AVERAGE_WINDOW_SIZE) {
            rssiValues.poll();
        }
        rssiValues.add(rssi);

        int sum = 0;
        for (int value : rssiValues) {
            sum += value;
        }
        return sum / rssiValues.size();
    }

    private int calculateSignalStrengthPercentage(int rssi) {
        int smoothedRssi = calculateSmoothedSignalStrength(rssi);
        int minRssi = -100;
        int maxRssi = -50;
        int percentage = (int) (((float) (smoothedRssi - minRssi) / (maxRssi - minRssi)) * 100);
        return Math.min(100, Math.max(0, percentage));
    }

    // Add a LeScanCallback to handle RSSI updates
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            int signalStrength = calculateSignalStrengthPercentage(rssi);
            Message msg = mHandler.obtainMessage(ConstantsForFindLocation.MESSAGE_RSSI, signalStrength, -1);
            mHandler.sendMessage(msg);
        }
    };

    // Add this method to get the latest signal strength
    public int getLatestSignalStrength() {
        // Assuming you have a way to get the latest RSSI value
        // For example, you can return the last calculated signal strength percentage
        return calculateSignalStrengthPercentage(lastRssiValue);
    }

    // Assuming you have a variable to store the last RSSI value
    private int lastRssiValue = -100; // Default value, update this with actual RSSI values

//    Nigel Update 11/26/2024 4:56 AM
}