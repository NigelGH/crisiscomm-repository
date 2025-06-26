package com.example.capstone.ui.chat.bluetooth;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.capstone.ui.chat.bluetooth.Message.RemoteMessage;
import com.example.capstone.ui.chat.chatting.DeviceConnectionState;

public class ChatServer {

    private static final String TAG = "ChatServer";
    private static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid MESSAGE_UUID = ParcelUuid.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b");
    private static final ParcelUuid CONFIRM_UUID = ParcelUuid.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928");

    private static Application app;
    private static BluetoothManager bluetoothManager;
    private static BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothLeAdvertiser advertiser;
    private static AdvertiseCallback advertiseCallback;
    private static AdvertiseSettings advertiseSettings = buildAdvertiseSettings();
    private static AdvertiseData advertiseData = buildAdvertiseData();

    private static MutableLiveData<Message> _messages = new MutableLiveData<>();
    public static LiveData<Message> messages = _messages;

    private static MutableLiveData<BluetoothDevice> _connectionRequest = new MutableLiveData<>();
    public static LiveData<BluetoothDevice> connectionRequest = _connectionRequest;

    private static MutableLiveData<Boolean> _requestEnableBluetooth = new MutableLiveData<>();
    public static LiveData<Boolean> requestEnableBluetooth = _requestEnableBluetooth;

    private static BluetoothGattServer gattServer;
    private static BluetoothGattServerCallback gattServerCallback;

    private static BluetoothGatt gattClient;
    private static BluetoothGattCallback gattClientCallback;

    private static BluetoothDevice currentDevice;
    private static MutableLiveData<DeviceConnectionState> _deviceConnection = new MutableLiveData<>();
    public static LiveData<DeviceConnectionState> deviceConnection = _deviceConnection;
    private static BluetoothGatt gatt;
    private static BluetoothGattCharacteristic messageCharacteristic;

    public static void startServer(Application application) {
        app = application;
        bluetoothManager = (BluetoothManager) app.getSystemService(Context.BLUETOOTH_SERVICE);
        if (!adapter.isEnabled()) {
            _requestEnableBluetooth.setValue(true);
        } else {
            _requestEnableBluetooth.setValue(false);
            setupGattServer(app);
            startAdvertisement();
        }
    }

    public static void stopServer() {
        stopAdvertising();
    }

    public static String getYourDeviceAddress() {
        return bluetoothManager.getAdapter().getAddress();
    }

    public static void setCurrentChatConnection(BluetoothDevice device) {
        currentDevice = device;
        _deviceConnection.setValue(new DeviceConnectionState.Connected(device));
        connectToChatDevice(device);
    }

    private static void connectToChatDevice(BluetoothDevice device) {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gattClientCallback = new GattClientCallback();
            gattClient = device.connectGatt(app, false, gattClientCallback);
        } else {
            Log.e(TAG, "Bluetooth connect permission not granted");
        }
    }

    public static boolean sendMessage(String message) {
        Log.d(TAG, "Send a message");
        if (messageCharacteristic != null) {
            messageCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            byte[] messageBytes = message.getBytes();
            messageCharacteristic.setValue(messageBytes);
            if (gatt != null) {
                if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    boolean success = gatt.writeCharacteristic(messageCharacteristic);
                    Log.d(TAG, "onServicesDiscovered: message send: " + success);
                    if (success) {
                        _messages.setValue(new Message.LocalMessage(message));
                    }
                } else {
                    Log.e(TAG, "Bluetooth connect permission not granted");
                }
            } else {
                Log.d(TAG, "sendMessage: no gatt connection to send a message with");
            }
        }
        return false;
    }

    private static void setupGattServer(Application app) {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            gattServerCallback = new GattServerCallback();
            gattServer = bluetoothManager.openGattServer(app, gattServerCallback);
            gattServer.addService(setupGattService());
        } else {
            Log.e(TAG, "Bluetooth admin permission not granted");
        }
    }

    private static BluetoothGattService setupGattService() {
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID.getUuid(), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic messageCharacteristic = new BluetoothGattCharacteristic(
                MESSAGE_UUID.getUuid(),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        service.addCharacteristic(messageCharacteristic);
        BluetoothGattCharacteristic confirmCharacteristic = new BluetoothGattCharacteristic(
                CONFIRM_UUID.getUuid(),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        service.addCharacteristic(confirmCharacteristic);
        return service;
    }

    private static void startAdvertisement() {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            advertiser = adapter.getBluetoothLeAdvertiser();
            Log.d(TAG, "startAdvertisement: with advertiser " + advertiser);
            if (advertiseCallback == null) {
                advertiseCallback = new DeviceAdvertiseCallback();
                advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
            }
        } else {
            Log.e(TAG, "Bluetooth advertise permission not granted");
        }
    }

    private static void stopAdvertising() {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Stopping Advertising with advertiser " + advertiser);
            if (advertiser != null) {
                advertiser.stopAdvertising(advertiseCallback);
            }
            advertiseCallback = null;
        } else {
            Log.e(TAG, "Bluetooth advertise permission not granted");
        }
    }

    private static AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .addServiceUuid(SERVICE_UUID)
                .setIncludeDeviceName(true);
        return dataBuilder.build();
    }

    private static AdvertiseSettings buildAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTimeout(0)
                .build();
    }

    public static LiveData<BluetoothDevice> getConnectionRequest() {
        return connectionRequest;
    }

    public static LiveData<DeviceConnectionState> getDeviceConnection() {
        return deviceConnection;
    }

    public static LiveData<Message> getMessages() {
        return messages;
    }

    private static class GattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            boolean isSuccess = status == BluetoothGatt.GATT_SUCCESS;
            boolean isConnected = newState == BluetoothProfile.STATE_CONNECTED;
            Log.d(TAG, "onConnectionStateChange: Server " + device + " " + (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED ? device.getName() : "Permission required") + " success: " + isSuccess + " connected: " + isConnected);
            if (isSuccess && isConnected) {
                _connectionRequest.postValue(device);
            } else {
                _deviceConnection.postValue(DeviceConnectionState.Disconnected.INSTANCE);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            if (characteristic.getUuid().equals(MESSAGE_UUID.getUuid())) {
                if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                } else {
                    Log.e(TAG, "Bluetooth connect permission not granted");
                }
                String message = new String(value);
                Log.d(TAG, "onCharacteristicWriteRequest: Have message: \"" + message + "\"");
                if (message != null) {
                    _messages.postValue(new RemoteMessage(message));
                }
            }
        }
    }

    private static class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean isSuccess = status == BluetoothGatt.GATT_SUCCESS;
            boolean isConnected = newState == BluetoothProfile.STATE_CONNECTED;
            Log.d(TAG, "onConnectionStateChange: Client " + gatt + " success: " + isSuccess + " connected: " + isConnected);
            if (isSuccess && isConnected) {
                if (ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                } else {
                    Log.e(TAG, "Bluetooth connect permission not granted");
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt discoveredGatt, int status) {
            super.onServicesDiscovered(discoveredGatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: Have gatt " + discoveredGatt);
                gatt = discoveredGatt;
                BluetoothGattService service = discoveredGatt.getService(SERVICE_UUID.getUuid());
                messageCharacteristic = service.getCharacteristic(MESSAGE_UUID.getUuid());
            }
        }
    }

    private static class DeviceAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            String errorMessage = "Advertise failed with error: " + errorCode;
            Log.d(TAG, "Advertising failed");
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }
    }
}