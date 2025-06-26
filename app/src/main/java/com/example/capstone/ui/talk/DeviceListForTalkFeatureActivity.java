package com.example.capstone.ui.talk;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.capstone.R;

import java.util.ArrayList;
import java.util.Set;

public class DeviceListForTalkFeatureActivity extends Activity {
    private static final String TAG = "DeviceListForTalkFeatureActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private DeviceListAdapterForTalkFeature mNewDevicesArrayAdapter;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list_for_talk_feature);

        setResult(Activity.RESULT_CANCELED);

        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (checkAndRequestPermissions()) {
                    doDiscovery();
                    v.setVisibility(View.GONE);
                }
            }
        });

        ArrayList<String> pairedDevicesList = new ArrayList<>();
        ArrayList<String> newDevicesList = new ArrayList<>();

        DeviceListAdapterForTalkFeature pairedDevicesArrayAdapter = new DeviceListAdapterForTalkFeature(this, R.layout.device_name_for_talk_feature, pairedDevicesList);
        mNewDevicesArrayAdapter = new DeviceListAdapterForTalkFeature(this, R.layout.device_name_for_talk_feature, newDevicesList);

        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (checkAndRequestPermissions()) {
            Set<BluetoothDevice> pairedDevices = getPairedDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                for (BluetoothDevice device : pairedDevices) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        int deviceClass = device.getBluetoothClass().getDeviceClass();
                        if (deviceClass == BluetoothClass.Device.PHONE_SMART) {
                            pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        }
                    }
                }
            } else {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                String noDevices = getResources().getText(R.string.none_paired).toString();
                pairedDevicesArrayAdapter.add(noDevices);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null && checkAndRequestPermissions()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                mBtAdapter.cancelDiscovery();
            }
        }
        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery() {
        if (!checkAndRequestPermissions()) {
            return;
        }

        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // Clear the new devices list
        mNewDevicesArrayAdapter.clear();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
            mBtAdapter.startDiscovery();
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            String info = ((TextView) v.findViewById(R.id.device_name)).getText().toString();
            if (info.equals(getResources().getString(R.string.none_paired)) || info.equals(getResources().getString(R.string.none_found))) {
                return; // Ignore clicks on the "none_paired" or "none_found" items
            }

            if (!checkAndRequestPermissions()) {
                return;
            }

            if (ContextCompat.checkSelfPermission(DeviceListForTalkFeatureActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                mBtAdapter.cancelDiscovery();
            }

            String address = info.substring(info.length() - 17);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    int deviceClass = device.getBluetoothClass().getDeviceClass();
                    if (deviceClass == BluetoothClass.Device.PHONE_SMART) {
                        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                            mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
                findViewById(R.id.button_scan).setVisibility(View.VISIBLE); // Make scanning button visible again
            }
        }
    };

    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with the action
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for this app", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Set<BluetoothDevice> getPairedDevices() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return mBtAdapter.getBondedDevices();
        }
        return null;
    }
}