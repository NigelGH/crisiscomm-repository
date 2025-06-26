package com.example.capstone.ui.find;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FindViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public FindViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Find feature fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}