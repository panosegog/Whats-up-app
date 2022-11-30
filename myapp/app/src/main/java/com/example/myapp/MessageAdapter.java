package com.example.myapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class MessageAdapter extends ArrayAdapter<Message> {

    private Context context;
    private ArrayList<Message> messages = new ArrayList<>();
    private String username;
    public MessageAdapter(@NonNull Context context, String username) {
        super(context, 0);
        this.context = context;
        this.username = username;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Message message = messages.get(position);
        switch (message.getType()) {
            case Text: return getTextView(message, convertView, parent);
            case Image: return getImageView(message, convertView, parent);
            case Video: return getVideoView(message, convertView, parent);
            default: return null;
        }
    }

    @Override
    public int getCount() { return messages == null ? 0 : messages.size(); }

    @Override
    public Message getItem(int position) { return messages == null ? null : messages.get(position); }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void appendMessage(Message message) {
        messages.add(message);
        notifyDataSetChanged();
    }

    @NonNull
    private View getTextView(Message message, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.text_message_item, parent, false);
        ConstraintLayout constraintLayout = convertView.findViewById(R.id.constraintLayoutTextMessage);
        ConstraintLayout parentConstraint  = convertView.findViewById(R.id.constraintLayoutTextMessageItem);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentConstraint);
        constraintSet.connect(R.id.constraintLayoutTextMessage, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP,0);
        if(message.getUsername().compareTo(username) == 0) {//Sender is me
            constraintLayout.setBackgroundResource(R.drawable.bg_message_sent);
            constraintSet.connect(R.id.constraintLayoutTextMessage, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,0);
        }
        else {//Other ppl send message
            constraintLayout.setBackgroundResource(R.drawable.bg_message_recvieved);
            constraintSet.connect(R.id.constraintLayoutTextMessage, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT,0);
        }
        constraintSet.applyTo(parentConstraint);

        TextView sender = convertView.findViewById(R.id.textViewTextSender);
        sender.setText(message.getUsername()+":");
        TextView content = convertView.findViewById(R.id.textViewMessage);
        content.setText((String)message.getContent());
        return convertView;
    }

    @NonNull
    private View getImageView(Message message, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.image_message_item, parent, false);
        ConstraintLayout constraintLayout = convertView.findViewById(R.id.constraintLayoutImageMessage);
        ConstraintLayout parentConstraint  = convertView.findViewById(R.id.constraintLayoutVideoMessageItem);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(parentConstraint);
        constraintSet.connect(R.id.constraintLayoutImageMessage, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP,0);
        if(message.getUsername().compareTo(username) == 0) {//Sender is me
            constraintLayout.setBackgroundResource(R.drawable.bg_message_sent);
            constraintSet.connect(R.id.constraintLayoutImageMessage, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,0);
        }
        else {//Other ppl send message
            constraintLayout.setBackgroundResource(R.drawable.bg_message_recvieved);
            constraintSet.connect(R.id.constraintLayoutImageMessage, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT,0);
        }
        constraintSet.applyTo(parentConstraint);

        TextView sender = convertView.findViewById(R.id.textViewImageSender);
        sender.setText(message.getUsername()+":");

        MediaFile media = (MediaFile) message.getContent();

        ImageView content = convertView.findViewById(R.id.imageViewMessage);
        content.setVisibility(View.VISIBLE);
        ProgressBar bar = convertView.findViewById(R.id.progressBarImageMedia);
        bar.setVisibility(View.VISIBLE);

        File file = new File(context.getExternalFilesDir(null),username+"/pictures/"+media.getName()+".jpg");
        final float aspectRatio = media.getWidth()/(float)media.getHeight();
        if(file.length() == media.getSize()) {
            bar.setVisibility(View.INVISIBLE);
            final int width = parentConstraint.getWidth() - 48;
            final int height = (int)(width * aspectRatio);
            Picasso.get().load(file)
                    .resize(width, height)
                    .onlyScaleDown()
                    .centerInside()
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .into(content);

        }
        else {
            Picasso.get().load(R.drawable.icon_image_placeholder)
                    .resize(256, 256)
                    .placeholder(R.drawable.icon_image_placeholder)
                    .into(content);
        }
        return convertView;
    }

    @NonNull
    private View getVideoView(Message message, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.video_message_item, parent, false);
        ConstraintLayout constraintLayout = convertView.findViewById(R.id.constraintLayoutVideoMessageItem);

        if(message.getUsername().compareTo(username) == 0) {//Sender is me
            constraintLayout.setBackgroundResource(R.drawable.bg_message_sent);
        }
        else {//Other ppl send message
            constraintLayout.setBackgroundResource(R.drawable.bg_message_recvieved);
        }

        TextView sender = convertView.findViewById(R.id.textViewVideoSender);
        sender.setText(message.getUsername()+":");

        MediaFile media = (MediaFile) message.getContent();

        ImageView placeholder = convertView.findViewById(R.id.imageViewVideoMessage);
        ProgressBar bar = convertView.findViewById(R.id.progressBarVideoMedia);
        VideoView content = convertView.findViewById(R.id.videoViewMessage);

        File file = new File(context.getExternalFilesDir(null),username+"/videos/"+media.getName()+".mp4");
        final float aspectRatio = media.getWidth()/(float)media.getHeight();
        if(file.length() == media.getSize()) {
            bar.setVisibility(View.INVISIBLE);
            placeholder.setVisibility(View.INVISIBLE);
            if(file.exists()) {
                content.setVideoPath(file.getPath());
                content.setVisibility(View.VISIBLE);
            }
        }
        else {
            placeholder.setVisibility(View.VISIBLE);
            bar.setVisibility(View.VISIBLE);
            content.setVisibility(View.INVISIBLE);
            Picasso.get().load(R.drawable.icon_movie_placeholder)
                    .resize(256, 256)
                    .placeholder(R.drawable.icon_movie_placeholder)
                    .into(placeholder);
        }
        return convertView;
    }
}
