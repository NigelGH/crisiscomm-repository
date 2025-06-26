package com.example.capstone.ui.find;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.capstone.databinding.FragmentFindBinding;

public class FindFragment extends Fragment {

    private FragmentFindBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFindBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button startFindButton = binding.startFindButton;
        startFindButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartFindLocationActivity.class);
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