package com.example.capstone.ui.chat.scanning;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.capstone.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanAdapter extends RecyclerView.Adapter<DeviceScanViewHolder> {

    private final DeviceScanViewHolder.OnDeviceSelectedListener onDeviceSelected;
    private List<BluetoothDevice> items = new ArrayList<>();

    public DeviceScanAdapter(DeviceScanViewHolder.OnDeviceSelectedListener onDeviceSelected) {
        this.onDeviceSelected = onDeviceSelected;
    }

    @NonNull
    @Override
    public DeviceScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_device, parent, false);
        return new DeviceScanViewHolder(view, onDeviceSelected);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceScanViewHolder holder, int position) {
        if (position < items.size()) {
            BluetoothDevice device = items.get(position);
            holder.bind(device);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<BluetoothDevice> results) {
        items = results;
        notifyDataSetChanged();
    }
}