package com.example.capstone.ui.chat.scanning;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.capstone.ui.chat.bluetooth.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceScanViewModel extends AndroidViewModel {

    private static final String TAG = "DeviceScanViewModel";
    private static final long SCAN_PERIOD = 12000L; // 12-second scanning period

    private final MutableLiveData<DeviceScanViewState> _viewState = new MutableLiveData<>();
    public LiveData<DeviceScanViewState> viewState = _viewState;

    private final Map<String, BluetoothDevice> scanResults = new HashMap<>();

    private final BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private DeviceScanCallback scanCallback;
    private final List<ScanFilter> scanFilters;
    private final ScanSettings scanSettings;
    private final Handler handler = new Handler();

    public DeviceScanViewModel(@NonNull Application app) {
        super(app);

        // Initialize Bluetooth adapter and scanning filters/settings
        adapter = BluetoothAdapter.getDefaultAdapter();
        scanFilters = buildScanFilters();
        scanSettings = buildScanSettings();

        // Start a scanning for BLE devices
        startScan();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopScanning();  // Ensure scanning is stopped when ViewModel is cleared
    }

    public void startScan() {
        if (!adapter.isMultipleAdvertisementSupported()) {
            _viewState.setValue(new DeviceScanViewState.AdvertisementNotSupported());
            return;
        }

        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            if (scanCallback == null) {
                scanner = adapter.getBluetoothLeScanner();
                Log.d(TAG, "Start Scanning");
                _viewState.setValue(new DeviceScanViewState.ActiveScan());

                // Stop scanning after the scanning period
                handler.postDelayed(this::stopScanning, SCAN_PERIOD);

                scanCallback = new DeviceScanCallback();
                scanner.startScan(scanFilters, scanSettings, scanCallback);
            } else {
                Log.d(TAG, "Already scanning");
            }
        } else {
            Log.e(TAG, "Bluetooth scanning permission not granted");
            _viewState.setValue(new DeviceScanViewState.Error("Bluetooth scanning permission not granted"));
        }
    }

    private void stopScanning() {
        Log.d(TAG, "Stopping Scanning");
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            if (scanner != null && scanCallback != null) {
                scanner.stopScan(scanCallback);
                _viewState.setValue(new DeviceScanViewState.ScanResults(scanResults));
            }
            scanCallback = null;
        } else {
            Log.e(TAG, "Bluetooth scanning permission not granted");
            _viewState.setValue(new DeviceScanViewState.Error("Bluetooth scanning permission not granted"));
        }
    }

    private List<ScanFilter> buildScanFilters() {
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(Constants.SERVICE_UUID))
                .build();
        return List.of(filter);
    }

    private ScanSettings buildScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
    }

    private class DeviceScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    scanResults.put(device.getAddress(), device);
                }
            }
            _viewState.setValue(new DeviceScanViewState.ScanResults(scanResults));
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                scanResults.put(device.getAddress(), device);
            }
            _viewState.setValue(new DeviceScanViewState.ScanResults(scanResults));
        }

        @Override
        public void onScanFailed(int errorCode) {
            String errorMessage = "Scan failed with error: " + errorCode;
            _viewState.setValue(new DeviceScanViewState.Error(errorMessage));
        }
    }
}