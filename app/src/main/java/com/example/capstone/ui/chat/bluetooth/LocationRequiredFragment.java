package com.example.capstone.ui.chat.bluetooth;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.capstone.R;
import com.example.capstone.databinding.FragmentLocationRequiredBinding;

public class LocationRequiredFragment extends Fragment {

    private static final String TAG = "LocationRequiredFrag";
    private static final int LOCATION_REQUEST_CODE = 0;
    private FragmentLocationRequiredBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLocationRequiredBinding.inflate(inflater, container, false);

        // Hide the error messages while checking the permissions
        binding.locationErrorMessage.setVisibility(View.GONE);
        binding.grantPermissionButton.setVisibility(View.GONE);

        // Setup click listener on grant permission button
        binding.grantPermissionButton.setOnClickListener(v -> checkLocationPermission());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check location permission when the Fragment becomes visible on screen
        checkLocationPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: ");
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Navigate to the chatting fragment
                NavHostFragment.findNavController(this).navigate(R.id.action_start_chat);
            } else {
                showError();
            }
        }
    }

    private void showError() {
        binding.locationErrorMessage.setVisibility(View.VISIBLE);
        binding.grantPermissionButton.setVisibility(View.VISIBLE);
    }

    private void checkLocationPermission() {
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (hasLocationPermission) {
            // Navigate to the chatting fragment
            NavHostFragment.findNavController(this).navigate(R.id.action_start_chat);
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE
            );
        }
    }
}