package com.example.capstone.ui.chat.chatting;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.capstone.R;
import com.example.capstone.ui.chat.bluetooth.Message;

public class RemoteMessageViewHolder extends RecyclerView.ViewHolder {

    private final TextView messageText;

    public RemoteMessageViewHolder(View view) {
        super(view);
        messageText = view.findViewById(R.id.message_text);
    }

    public void bind(Message.RemoteMessage message) {
        messageText.setText(message.getText());
    }
}
