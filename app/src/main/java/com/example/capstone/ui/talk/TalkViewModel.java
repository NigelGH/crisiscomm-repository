package com.example.capstone.ui.talk;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TalkViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public TalkViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Talk feature fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}