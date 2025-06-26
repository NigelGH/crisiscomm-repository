package com.example.capstone.ui.talk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.capstone.R;

import java.util.List;

public class DeviceListAdapterForTalkFeature extends ArrayAdapter<String> {
    private final int resourceLayout;
    private final Context mContext;

    public DeviceListAdapterForTalkFeature(Context context, int resource, List<String> items) {
        super(context, resource, items);
        this.resourceLayout = resource;
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            view = vi.inflate(resourceLayout, null);
        }

        String deviceInfo = getItem(position);
        if (deviceInfo != null) {
            ImageView deviceIcon = view.findViewById(R.id.device_icon);
            TextView deviceName = view.findViewById(R.id.device_name);

            if (deviceIcon != null) {
                if (deviceInfo.equals(mContext.getString(R.string.none_paired)) || deviceInfo.equals(mContext.getString(R.string.none_found))) {
                    deviceIcon.setVisibility(View.GONE);
                } else {
                    deviceIcon.setVisibility(View.VISIBLE);
                    deviceIcon.setImageResource(R.drawable.ic_baseline_smartphone);
                }
            }

            if (deviceName != null) {
                deviceName.setText(deviceInfo);
            }
        }

        return view;
    }
}