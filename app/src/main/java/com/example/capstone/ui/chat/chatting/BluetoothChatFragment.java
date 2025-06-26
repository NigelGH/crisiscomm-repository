package com.example.capstone.ui.chat.chatting;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.capstone.R;
import com.example.capstone.ui.chat.bluetooth.ChatServer;
import com.example.capstone.ui.chat.bluetooth.Message;
import com.example.capstone.databinding.FragmentBluetoothChatBinding;

public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private FragmentBluetoothChatBinding binding;
    private MessageAdapter adapter = new MessageAdapter();
    private InputMethodManager inputMethodManager;

    private final Observer<DeviceConnectionState> deviceConnectionObserver = new Observer<DeviceConnectionState>() {
        @Override
        public void onChanged(DeviceConnectionState state) {
            if (state instanceof DeviceConnectionState.Connected) {
                BluetoothDevice device = ((DeviceConnectionState.Connected) state).getDevice();
                Log.d(TAG, "Gatt connection observer: have device " + device);
                chatWith(device);
            } else if (state instanceof DeviceConnectionState.Disconnected) {
                showDisconnected();
            }
        }
    };

    private final Observer<BluetoothDevice> connectionRequestObserver = new Observer<BluetoothDevice>() {
        @Override
        public void onChanged(BluetoothDevice device) {
            Log.d(TAG, "Connection request observer: have device " + device);
            ChatServer.setCurrentChatConnection(device);
        }
    };

    private final Observer<Message> messageObserver = new Observer<Message>() {
        @Override
        public void onChanged(Message message) {
            Log.d(TAG, "Have message " + message.getText());
            adapter.addMessage(message);
            scrollToBottom();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBluetoothChatBinding.inflate(inflater, container, false);
        inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        Log.d(TAG, "chatWith: set adapter " + adapter);
        binding.messages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.messages.setAdapter(adapter);

        showDisconnected();

        binding.connectDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(BluetoothChatFragment.this).navigate(R.id.action_find_new_device);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().setTitle(R.string.chat_title);
        ChatServer.getConnectionRequest().observe(getViewLifecycleOwner(), connectionRequestObserver);
        ChatServer.getDeviceConnection().observe(getViewLifecycleOwner(), deviceConnectionObserver);
        ChatServer.getMessages().observe(getViewLifecycleOwner(), messageObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void chatWith(BluetoothDevice device) {
        binding.connectedContainer.setVisibility(View.VISIBLE);
        binding.notConnectedContainer.setVisibility(View.GONE);

        String chattingWithString = getResources().getString(R.string.chatting_with_device, device.getAddress());
        binding.connectedDeviceName.setText(chattingWithString);

        if (getActivity() != null) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                String deviceName = device.getName();
                if (deviceName != null && !deviceName.isEmpty()) {
                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    if (activity.getSupportActionBar() != null) {
                        activity.getSupportActionBar().setSubtitle(deviceName);
                    } else {
                        Log.e(TAG, "Action bar is null");
                    }
                } else {
                    Log.e(TAG, "Device name is null or empty");
                }
            } else {
                Log.e(TAG, "Bluetooth connect permission not granted");
            }
        } else {
            Log.e(TAG, "Activity is null");
        }

        binding.sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = binding.messageText.getText().toString();
                if (!message.isEmpty()) {
                    ChatServer.sendMessage(message);
                    binding.messageText.setText("");
                    scrollToBottom();
                }
            }
        });
    }

    private void showDisconnected() {
        hideKeyboard();
        binding.notConnectedContainer.setVisibility(View.VISIBLE);
        binding.connectedContainer.setVisibility(View.GONE);

        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(null);
        }
    }

    private void hideKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
    }

    private void scrollToBottom() {
        binding.messages.scrollToPosition(adapter.getItemCount() - 1);
    }
}