package com.example.capstone.ui.chat.chatting;

import android.bluetooth.BluetoothDevice;


public abstract class DeviceConnectionState {

    public static class Connected extends DeviceConnectionState {
        private final BluetoothDevice device;

        public Connected(BluetoothDevice device) {
            this.device = device;
        }

        public BluetoothDevice getDevice() {
            return device;
        }
    }

    public static class Disconnected extends DeviceConnectionState {
        public static final Disconnected INSTANCE = new Disconnected();

        private Disconnected() {
            // Private constructor to enforce singleton pattern
        }
    }
}
