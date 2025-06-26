package com.example.capstone.ui.find;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.capstone.R;

import java.io.IOException;

public class BluetoothFindLocationFragment extends Fragment {

    private static final String TAG = "BluetoothFindLocationFragment";

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothFindLocationService mChatService = null;
    private MediaPlayer mediaPlayer = null;
    private CameraManager cameraManager = null;
    private String cameraId = null;
    private Handler flashHandler = new Handler();
    private boolean isFlashOn = false;
    private int sosIndex = 0;
    private long[] sosPattern = {200, 200, 200, 600, 600, 600, 200, 200, 200, 1000}; // SOS pattern in milliseconds
    private Runnable flashRunnable;
    private boolean isLocalSoundPlaying = false;
    private ProgressBar signalStrengthProgressBar;
    private TextView signalStrengthStatusTextView;
    private TextView signalStrengthPercentageTextView;

    // Add these fields to the BluetoothFindLocationFragment class
    private Handler uiUpdateHandler = new Handler();
    private Runnable uiUpdateRunnable;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_bluetooth_find_location, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set the action bar title
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Find");
        }

        Button makeSoundButton = view.findViewById(R.id.make_sound_button);
        Button disconnectButton = view.findViewById(R.id.disconnect_button);
        signalStrengthProgressBar = view.findViewById(R.id.signal_strength_progress);
        signalStrengthStatusTextView = view.findViewById(R.id.signal_strength_status);
        signalStrengthPercentageTextView = view.findViewById(R.id.signal_strength_percentage);
        TextView notConnectedMessage = view.findViewById(R.id.not_connected_message);

        makeSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                if (button.getText().equals("Make Sound")) {
                    sendMessage("MAKE_SOUND");
                    button.setText("Stop Sound");
                } else {
                    sendMessage("STOP_SOUND");
                    button.setText("Make Sound");
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChatService != null) {
                    sendMessage("STOP_SOUND");
                    mChatService.stop();
                }
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                flashHandler.removeCallbacks(flashRunnable);
                turnOffFlashlight();
                makeSoundButton.setText("Make Sound");
                resetUI();
            }
        });

        updateUI(false, null); // Initially set UI to not connected state
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        FragmentActivity activity = getActivity();
        if (mBluetoothAdapter == null && activity != null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

        cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        flashRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFlashOn) {
                    turnOffFlashlight();
                } else {
                    turnOnFlashlight();
                }
                isFlashOn = !isFlashOn;
                flashHandler.postDelayed(this, sosPattern[sosIndex]);
                sosIndex = (sosIndex + 1) % sosPattern.length;
            }
        };

        // Initialize the Runnable for UI updates
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Update the UI with the latest signal strength values
                updateSignalStrengthUI();
                // Schedule the next update
                uiUpdateHandler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mBluetoothAdapter == null) {
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            sendMessage("STOP_SOUND");
            mChatService.stop();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        flashHandler.removeCallbacks(flashRunnable);
        turnOffFlashlight();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothFindLocationService.STATE_NONE) {
                mChatService.start();
            }
        }
        // Start the UI updates
        uiUpdateHandler.post(uiUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop the UI updates
        uiUpdateHandler.removeCallbacks(uiUpdateRunnable);
    }

    private void setupChat() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mChatService = new BluetoothFindLocationService(activity, mHandler);
    }

    private void sendMessage(String message) {
        if (mChatService.getState() != BluetoothFindLocationService.STATE_CONNECTED) {
//            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    private void updateUI(boolean isConnected, @Nullable String deviceName) {
        if (rootView != null) {
            int visibility = isConnected ? View.VISIBLE : View.GONE;
            rootView.findViewById(R.id.signal_strength_progress).setVisibility(visibility);
            rootView.findViewById(R.id.signal_strength_status).setVisibility(visibility);
            rootView.findViewById(R.id.signal_strength_percentage).setVisibility(visibility);
            rootView.findViewById(R.id.title).setVisibility(visibility);
            rootView.findViewById(R.id.make_sound_button).setVisibility(visibility);
            rootView.findViewById(R.id.disconnect_button).setVisibility(visibility);
            rootView.findViewById(R.id.not_connected_message).setVisibility(isConnected ? View.GONE : View.VISIBLE);

            TextView titleTextView = rootView.findViewById(R.id.title);
            if (isConnected && deviceName != null) {
                titleTextView.setText("Find " + deviceName);
            } else {
                titleTextView.setText("Find Connected Device");
            }
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case ConstantsForFindLocation.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothFindLocationService.STATE_CONNECTED:
                            BluetoothDevice connectedDevice = mChatService.getConnectedDevice();
                            if (connectedDevice != null) {
                                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    updateUI(true, connectedDevice.getName());
                                } else {
                                    updateUI(true, null);
                                    Toast.makeText(getActivity(), "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show();
                                }
                            }
                            activity.invalidateOptionsMenu();
                            break;
                        case BluetoothFindLocationService.STATE_CONNECTING:
                            break;
                        case BluetoothFindLocationService.STATE_LISTEN:
                        case BluetoothFindLocationService.STATE_NONE:
                            resetUI();
                            updateUI(false, null);
                            activity.invalidateOptionsMenu();
                            break;
                    }
                    break;
                case ConstantsForFindLocation.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage.equals("MAKE_SOUND")) {
                        playSound();
                        flashHandler.post(flashRunnable); // Start the SOS blinking pattern
                    } else if (readMessage.equals("STOP_SOUND")) {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                        flashHandler.removeCallbacks(flashRunnable); // Stop the SOS blinking pattern
                        turnOffFlashlight(); // Ensure the flashlight is turned off
                    }
                    break;
                case ConstantsForFindLocation.MESSAGE_RSSI:
                    int signalStrength = msg.arg1;
                    signalStrengthProgressBar.setProgress(signalStrength);
                    signalStrengthPercentageTextView.setText(signalStrength + "%");
                    LayerDrawable layerDrawable = (LayerDrawable) signalStrengthProgressBar.getProgressDrawable();
                    if (signalStrength >= 75) {
                        layerDrawable.findDrawableByLayerId(android.R.id.progress)
                                .setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
                    } else if (signalStrength >= 21) {
                        layerDrawable.findDrawableByLayerId(android.R.id.progress)
                                .setColorFilter(Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN);
                    } else {
                        layerDrawable.findDrawableByLayerId(android.R.id.progress)
                                .setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                    if (signalStrength < 30) {
                        signalStrengthStatusTextView.setText("Signal Strength: Weak");
                    } else if (signalStrength > 70) {
                        signalStrengthStatusTextView.setText("Signal Strength: Strong");
                    } else {
                        signalStrengthStatusTextView.setText("Signal Strength: Moderate");
                    }
                    break;
                case ConstantsForFindLocation.MESSAGE_TOAST:
                    Toast.makeText(activity, msg.getData().getString(ConstantsForFindLocation.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void resetUI() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            TextView titleTextView = activity.findViewById(R.id.title);
            titleTextView.setText("Find Connected Device");

            signalStrengthProgressBar.setProgress(0);
            signalStrengthStatusTextView.setText("Signal Strength: Weak");
            signalStrengthPercentageTextView.setText("0%");

            Button makeSoundButton = activity.findViewById(R.id.make_sound_button);
            makeSoundButton.setText("Make Sound");

            Button disconnectButton = activity.findViewById(R.id.disconnect_button);
            disconnectButton.setText("Disconnect");
        }
    }

    private void playSound() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(activity, Settings.System.DEFAULT_RINGTONE_URI);
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mediaPlayer.setLooping(false);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void turnOnFlashlight() {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, true);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void turnOffFlashlight() {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_bar_menu_options, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem secureConnectItem = menu.findItem(R.id.secure_connect_scan);
        MenuItem insecureConnectItem = menu.findItem(R.id.insecure_connect_scan);
        MenuItem discoverableItem = menu.findItem(R.id.discoverable);

        if (mChatService != null && mChatService.getState() == BluetoothFindLocationService.STATE_CONNECTED) {
            secureConnectItem.setVisible(false);
            insecureConnectItem.setVisible(false);
            discoverableItem.setVisible(false);
        } else {
            secureConnectItem.setVisible(true);
            insecureConnectItem.setVisible(true);
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                if (mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    discoverableItem.setVisible(false);
                } else {
                    discoverableItem.setVisible(true);
                }
            } else {
                discoverableItem.setVisible(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.secure_connect_scan) {
            Intent serverIntent = new Intent(getActivity(), DeviceListForFindLocationActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        } else if (id == R.id.insecure_connect_scan) {
            Intent serverIntent = new Intent(getActivity(), DeviceListForFindLocationActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        } else if (id == R.id.discoverable) {
            ensureDiscoverable();
            return true;
        }
        return false;
    }

    private void ensureDiscoverable() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            }
        } else {
            Toast.makeText(getActivity(), "Bluetooth scanning permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                                Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListForFindLocationActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }

    // Add a method to update the signal strength UI
    private void updateSignalStrengthUI() {
        // Get the latest signal strength values from the BluetoothFindLocationService
        int signalStrength = mChatService.getLatestSignalStrength();
        signalStrengthProgressBar.setProgress(signalStrength);
        signalStrengthPercentageTextView.setText(signalStrength + "%");
        LayerDrawable layerDrawable = (LayerDrawable) signalStrengthProgressBar.getProgressDrawable();
        if (signalStrength >= 75) {
            layerDrawable.findDrawableByLayerId(android.R.id.progress)
                    .setColorFilter(Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (signalStrength >= 21) {
            layerDrawable.findDrawableByLayerId(android.R.id.progress)
                    .setColorFilter(Color.YELLOW, android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            layerDrawable.findDrawableByLayerId(android.R.id.progress)
                    .setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        if (signalStrength < 30) {
            signalStrengthStatusTextView.setText("Signal Strength: Weak");
        } else if (signalStrength > 70) {
            signalStrengthStatusTextView.setText("Signal Strength: Strong");
        } else {
            signalStrengthStatusTextView.setText("Signal Strength: Moderate");
        }
    }

}