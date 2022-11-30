package com.example.myapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ChannelsAdapter extends ArrayAdapter<ChannelMeta> {

    private ArrayList<ChannelMeta> channels = null;
    private final Context context;

    public ChannelsAdapter(@NonNull Context context) {
        super(context, 0);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ChannelMeta channel = channels.get(position);
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.channel_item, parent, false);

        TextView channelName = convertView.findViewById(R.id.textViewChannelName);
        TextView newMessages = convertView.findViewById(R.id.textViewNewMessages);
        channelName.setText(channel.getChannelName());
        newMessages.setText("("+channel.getMessages()+")");
        return convertView;
    }

    public void setChannels(ArrayList<ChannelMeta> channels) {
        this.channels = channels;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return channels == null ? 0 : channels.size(); }

    @Override
    public ChannelMeta getItem(int position) { return channels == null ? null : channels.get(position); }
}
