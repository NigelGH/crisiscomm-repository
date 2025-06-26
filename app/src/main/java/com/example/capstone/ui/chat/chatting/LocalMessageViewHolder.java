package com.example.capstone.ui.chat.chatting;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.capstone.R;
import com.example.capstone.ui.chat.bluetooth.Message;

public class LocalMessageViewHolder extends RecyclerView.ViewHolder {

    private final TextView messageText;

    public LocalMessageViewHolder(View itemView) {
        super(itemView);
        messageText = itemView.findViewById(R.id.message_text);
    }

    public void bind(Message.LocalMessage message) {
        messageText.setText(message.getText());
    }
}
