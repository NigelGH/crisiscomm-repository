package com.example.capstone.ui.find;

public interface ConstantsForFindLocation {

    // Message types sent from the BluetoothFindLocationService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;
    int MESSAGE_RSSI = 6;

    // Key names received from the BluetoothFindLocationService Handler
    String DEVICE_NAME = "device_name_for_find_location";
    String TOAST = "toast";
}