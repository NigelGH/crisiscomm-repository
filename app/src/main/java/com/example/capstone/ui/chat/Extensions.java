package com.example.capstone.ui.chat;

import android.view.View;

public class Extensions {

    public static void visible(View view) {
        view.setVisibility(View.VISIBLE);
    }

    public static void gone(View view) {
        view.setVisibility(View.GONE);
    }

    public static <T> T exhaustive(T value) {
        return value;
    }
}
