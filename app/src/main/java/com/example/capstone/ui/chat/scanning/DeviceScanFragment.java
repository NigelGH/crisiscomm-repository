package com.example.capstone.ui.chat.scanning;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.capstone.R;
import com.example.capstone.ui.chat.bluetooth.ChatServer;
import com.example.capstone.databinding.FragmentDeviceScanBinding;

import java.util.List;
import java.util.Map;

public class DeviceScanFragment extends Fragment {

    private static final String TAG = "DeviceScanFragment";
    public static final String GATT_KEY = "gatt_bundle_key";

    private FragmentDeviceScanBinding binding;
    private DeviceScanViewModel viewModel;
    private DeviceScanAdapter deviceScanAdapter;
    private boolean isScanning = false;

    private final DeviceScanViewHolder.OnDeviceSelectedListener onDeviceSelected = new DeviceScanViewHolder.OnDeviceSelectedListener() {
        @Override
        public void onDeviceSelected(BluetoothDevice device) {
            ChatServer.setCurrentChatConnection(device);
            NavHostFragment.findNavController(DeviceScanFragment.this).popBackStack();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceScanBinding.inflate(inflater, container, false);
        binding.deviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        deviceScanAdapter = new DeviceScanAdapter(onDeviceSelected);
        binding.deviceList.setAdapter(deviceScanAdapter);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().setTitle(R.string.device_list_title);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DeviceScanViewModel.class);
        viewModel.viewState.observe(getViewLifecycleOwner(), viewStateObserver);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_device_scan, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem scanItem = menu.findItem(R.id.action_scan);
        scanItem.setVisible(!isScanning);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_scan) {
            viewModel.startScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLoading() {
        Log.d(TAG, "showLoading");
        isScanning = true;
        requireActivity().invalidateOptionsMenu();
        binding.scanning.setVisibility(View.VISIBLE);
        binding.deviceList.setVisibility(View.GONE);
        binding.noDevices.setVisibility(View.GONE);
        binding.error.setVisibility(View.GONE);
        binding.chatConfirmContainer.setVisibility(View.GONE);
    }

    private void showResults(Map<String, BluetoothDevice> scanResults) {
        isScanning = false;
        requireActivity().invalidateOptionsMenu();
        if (!scanResults.isEmpty()) {
            binding.deviceList.setVisibility(View.VISIBLE);
            deviceScanAdapter.updateItems(List.copyOf(scanResults.values()));
            binding.scanning.setVisibility(View.GONE);
            binding.noDevices.setVisibility(View.GONE);
            binding.error.setVisibility(View.GONE);
            binding.chatConfirmContainer.setVisibility(View.GONE);
        } else {
            showNoDevices();
        }
    }

    private void showNoDevices() {
        isScanning = false;
        requireActivity().invalidateOptionsMenu();
        binding.noDevices.setVisibility(View.VISIBLE);
        binding.deviceList.setVisibility(View.GONE);
        binding.scanning.setVisibility(View.GONE);
        binding.error.setVisibility(View.GONE);
        binding.chatConfirmContainer.setVisibility(View.GONE);
    }

    private void showError(String message) {
        isScanning = false;
        requireActivity().invalidateOptionsMenu();
        Log.d(TAG, "showError: ");
        binding.error.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(message);
        binding.errorAction.setVisibility(View.GONE);
        binding.scanning.setVisibility(View.GONE);
        binding.noDevices.setVisibility(View.GONE);
        binding.chatConfirmContainer.setVisibility(View.GONE);
    }

    private void showAdvertisingError() {
        showError("BLE advertising is not supported on this device");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private final Observer<DeviceScanViewState> viewStateObserver = new Observer<DeviceScanViewState>() {
        @Override
        public void onChanged(DeviceScanViewState viewState) {
            if (viewState instanceof DeviceScanViewState.ActiveScan) {
                showLoading();
            } else if (viewState instanceof DeviceScanViewState.ScanResults) {
                showResults(((DeviceScanViewState.ScanResults) viewState).getScanResults());
            } else if (viewState instanceof DeviceScanViewState.Error) {
                showError(((DeviceScanViewState.Error) viewState).getMessage());
            } else if (viewState instanceof DeviceScanViewState.AdvertisementNotSupported) {
                showAdvertisingError();
            }
        }
    };
}