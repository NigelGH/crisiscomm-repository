package com.example.capstone.ui.talk;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.capstone.R;

public class BluetoothTalkFragment extends Fragment {

    private static final String TAG = "BluetoothTalkFragment";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 4;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 2;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 3;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothTalkService mChatService = null;
    private AudioRecord mRecorder = null;

    private ImageButton mSpeakButton;
    private Button mDisconnectButton;
    private CurvedTextView mStatusText;
    private TextView mNotConnectedText;
    private Menu menu;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_talk, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Talk");
        }
        mSpeakButton = view.findViewById(R.id.speak_button);
        mDisconnectButton = view.findViewById(R.id.disconnect_button);
        mStatusText = view.findViewById(R.id.status_text);
        mNotConnectedText = view.findViewById(R.id.not_connected_text);

        mSpeakButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    AnimatorSet scaleDown = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.scale_down);
                    scaleDown.setTarget(mSpeakButton);
                    scaleDown.start();
                    mSpeakButton.setBackgroundResource(R.drawable.speak_button_pressed);
                    mSpeakButton.setImageResource(R.drawable.ic_baseline_graphic_eq);
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                    AnimatorSet scaleUp = (AnimatorSet) AnimatorInflater.loadAnimator(getActivity(), R.animator.scale_up);
                    scaleUp.setTarget(mSpeakButton);
                    scaleUp.start();
                    mSpeakButton.setBackgroundResource(R.drawable.circular_button);
                    mSpeakButton.setImageResource(R.drawable.ic_baseline_mic);
                    stopRecording();
                    return true;
            }
            return false;
        });

        mDisconnectButton.setOnClickListener(v -> {
            if (mChatService != null) {
                mChatService.stop();
            }
        });

        // Set custom action bar
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowCustomEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setCustomView(R.layout.custom_action_bar_talk_feature);
        }

        updateUI(mChatService != null && mChatService.getState() == com.example.capstone.ui.talk.BluetoothTalkService.STATE_CONNECTED);
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
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == com.example.capstone.ui.talk.BluetoothTalkService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        mChatService = new BluetoothTalkService(getActivity(), mHandler);
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        mRecorder.startRecording();
        mStatusText.setText("Speaking ...");

        new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                bytesRead = mRecorder.read(buffer, 0, buffer.length);
                if (bytesRead > 0 && mChatService.getState() == com.example.capstone.ui.talk.BluetoothTalkService.STATE_CONNECTED) {
                    mChatService.write(buffer);
                }
            }
        }).start();
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            mStatusText.setText("Press and hold to speak!");
        } else {
            Log.e(TAG, "stopRecording called but mRecorder is null");
        }
    }

    private void updateUI(boolean isConnected) {
        if (isConnected) {
            mSpeakButton.setVisibility(View.VISIBLE);
            mDisconnectButton.setVisibility(View.VISIBLE);
            mStatusText.setVisibility(View.VISIBLE);
            mNotConnectedText.setVisibility(View.GONE);
        } else {
            mSpeakButton.setVisibility(View.GONE);
            mDisconnectButton.setVisibility(View.GONE);
            mStatusText.setVisibility(View.GONE);
            mNotConnectedText.setVisibility(View.VISIBLE);
        }
        updateActionBarTitle(isConnected);
    }

    private void updateMenuItems(boolean isConnected) {
        if (menu != null) {
            menu.findItem(R.id.secure_connect_scan).setVisible(!isConnected);
            menu.findItem(R.id.insecure_connect_scan).setVisible(!isConnected);
            menu.findItem(R.id.discoverable).setVisible(!isConnected && !isDiscoverable());
        }
    }

    private void updateActionBarTitle(boolean isConnected) {
        if (getActivity() != null) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (isConnected && mChatService != null && mChatService.getConnectedDeviceName() != null) {
                activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
                activity.getSupportActionBar().setCustomView(R.layout.custom_action_bar_talk_feature);

                TextView titleView = activity.findViewById(R.id.action_bar_title);
                TextView subtitleView = activity.findViewById(R.id.action_bar_subtitle);

                subtitleView.setText(mChatService.getConnectedDeviceName());
                subtitleView.setVisibility(View.VISIBLE);
            } else {
                activity.getSupportActionBar().setDisplayShowCustomEnabled(false);
                activity.getSupportActionBar().setCustomView(null);
            }
        }
    }

    private boolean isDiscoverable() {
        if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
        }
        return false;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ConstantsForTalkFeatures.MESSAGE_STATE_CHANGE:
                    updateUI(msg.arg1 == com.example.capstone.ui.talk.BluetoothTalkService.STATE_CONNECTED);
                    updateMenuItems(msg.arg1 == com.example.capstone.ui.talk.BluetoothTalkService.STATE_CONNECTED);
                    break;
                case ConstantsForTalkFeatures.MESSAGE_WRITE:
                    break;
                case ConstantsForTalkFeatures.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    playAudio(readBuf);
                    break;
                case ConstantsForTalkFeatures.MESSAGE_DEVICE_NAME:
                    updateActionBarTitle(true);
                    break;
                case ConstantsForTalkFeatures.MESSAGE_TOAST:
                    Toast.makeText(getActivity(), msg.getData().getString(ConstantsForTalkFeatures.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void playAudio(byte[] audioData) {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size");
            return;
        }

        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .build();

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack initialization failed");
            return;
        }

        audioTrack.play();
        Log.d(TAG, "Playing audio data of size: " + audioData.length);
        audioTrack.write(audioData, 0, audioData.length);
        audioTrack.stop();
        audioTrack.release();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_bar_menu_options, menu);
        this.menu = menu;
        updateMenuItems(mChatService != null && mChatService.getState() == com.example.capstone.ui.talk.BluetoothTalkService.STATE_CONNECTED);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem discoverableItem = menu.findItem(R.id.discoverable);
        if (discoverableItem != null) {
            discoverableItem.setVisible(!isDiscoverable());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.secure_connect_scan) {
            Intent secureIntent = new Intent(getActivity(), DeviceListForTalkFeatureActivity.class);
            startActivityForResult(secureIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        } else if (id == R.id.insecure_connect_scan) {
            Intent insecureIntent = new Intent(getActivity(), DeviceListForTalkFeatureActivity.class);
            startActivityForResult(insecureIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        } else if (id == R.id.discoverable) {
            ensureDiscoverable();
            return true;
        }
        return false;
    }

    private void ensureDiscoverable() {
        if (getActivity() != null && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
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
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListForTalkFeatureActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }
}