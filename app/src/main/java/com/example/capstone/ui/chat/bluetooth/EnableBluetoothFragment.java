package com.example.capstone.ui.chat.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;

import com.example.capstone.R;
import com.example.capstone.databinding.FragmentEnableBluetoothBinding;

public class EnableBluetoothFragment extends Fragment {

    private static final int REQUEST_ENABLE_BT = 1;
    private FragmentEnableBluetoothBinding binding;

    private final Observer<Boolean> bluetoothEnableObserver = shouldPrompt -> {
        if (!shouldPrompt) {
            // Don't need to prompt so navigate to LocationRequiredFragment
            NavHostFragment.findNavController(EnableBluetoothFragment.this)
                    .navigate(R.id.action_check_location_permissions);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEnableBluetoothBinding.inflate(inflater, container, false);

        binding.errorAction.setOnClickListener(v -> {
            // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ChatServer.requestEnableBluetooth.observe(getViewLifecycleOwner(), bluetoothEnableObserver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                ChatServer.startServer(requireActivity().getApplication());
            }
        }
    }
}