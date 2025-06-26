package com.example.capstone.ui.talk;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.capstone.databinding.FragmentTalkBinding;

public class TalkFragment extends Fragment {

    private FragmentTalkBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTalkBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button startTalkButton = binding.startTalkButton;
        startTalkButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartTalkActivity.class);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}