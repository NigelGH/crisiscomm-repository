package com.example.capstone.ui.chat.scanning;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

public abstract class DeviceScanViewState {

    public static class ActiveScan extends DeviceScanViewState {
        // Empty class for ActiveScan state
    }

    public static class ScanResults extends DeviceScanViewState {
        private final Map<String, BluetoothDevice> scanResults;

        public ScanResults(Map<String, BluetoothDevice> scanResults) {
            this.scanResults = scanResults;
        }

        public Map<String, BluetoothDevice> getScanResults() {
            return scanResults;
        }
    }

    public static class Error extends DeviceScanViewState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class AdvertisementNotSupported extends DeviceScanViewState {
        // Empty class for AdvertisementNotSupported state
    }
}
