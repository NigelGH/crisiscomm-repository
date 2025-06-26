package com.example.capstone.ui.chat.scanning;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.capstone.R;

public class DeviceScanViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView name;
    private final TextView address;
    private BluetoothDevice bluetoothDevice;
    private final OnDeviceSelectedListener onDeviceSelected;

    public DeviceScanViewHolder(View view, OnDeviceSelectedListener onDeviceSelected) {
        super(view);
        this.onDeviceSelected = onDeviceSelected;
        name = view.findViewById(R.id.device_name);
        address = view.findViewById(R.id.device_address);
        view.setOnClickListener(this);
    }

    public void bind(BluetoothDevice device) {
        bluetoothDevice = device;
        if (ContextCompat.checkSelfPermission(itemView.getContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            name.setText(device.getName());
        } else {
            name.setText("Permission required");
        }
        address.setText(device.getAddress());
    }

    @Override
    public void onClick(View view) {
        if (bluetoothDevice != null) {
            onDeviceSelected.onDeviceSelected(bluetoothDevice);
        }
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }
}