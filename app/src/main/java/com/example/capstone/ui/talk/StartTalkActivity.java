package com.example.capstone.ui.talk;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.capstone.R;

public class StartTalkActivity extends AppCompatActivity {

    public static final String TAG = "StartTalkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_talk);

        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
        }

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothTalkFragment fragment = new BluetoothTalkFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
    }
}
