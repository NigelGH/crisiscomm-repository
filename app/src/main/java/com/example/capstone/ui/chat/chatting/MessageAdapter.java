package com.example.capstone.ui.chat.chatting;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.capstone.R;
import com.example.capstone.ui.chat.bluetooth.Message;
import com.example.capstone.ui.chat.chatting.LocalMessageViewHolder;
import com.example.capstone.ui.chat.chatting.RemoteMessageViewHolder;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessageAdapter";
    private static final int REMOTE_MESSAGE = 0;
    private static final int LOCAL_MESSAGE = 1;

    private final List<Message> messages = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: ");
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == REMOTE_MESSAGE) {
            view = inflater.inflate(R.layout.item_remote_message, parent, false);
            return new RemoteMessageViewHolder(view);
        } else if (viewType == LOCAL_MESSAGE) {
            view = inflater.inflate(R.layout.item_local_message, parent, false);
            return new LocalMessageViewHolder(view);
        } else {
            throw new IllegalArgumentException("Unknown MessageAdapter view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: ");
        Message message = messages.get(position);
        if (message instanceof Message.RemoteMessage) {
            ((RemoteMessageViewHolder) holder).bind((Message.RemoteMessage) message);
        } else if (message instanceof Message.LocalMessage) {
            ((LocalMessageViewHolder) holder).bind((Message.LocalMessage) message);
        }
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: ");
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Log.d(TAG, "getItemViewType: ");
        Message message = messages.get(position);
        if (message instanceof Message.RemoteMessage) {
            return REMOTE_MESSAGE;
        } else if (message instanceof Message.LocalMessage) {
            return LOCAL_MESSAGE;
        } else {
            throw new IllegalArgumentException("Unknown message type");
        }
    }

    // Add messages to the bottom of the list
    public void addMessage(Message message) {
        Log.d(TAG, "addMessage: ");
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
}