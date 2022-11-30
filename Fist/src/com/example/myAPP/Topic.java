package com.example.myAPP;

import java.io.Serializable;

public class Topic implements Serializable {
    private static final long serialVersionUID = 7710439065836944524L;
    // Fields
    private String channelName = "";
    private int messages = 0;

    // Constructor(s)
    public Topic(String channelName) { this.channelName = channelName; }
    public Topic(String channelName, int messages)
    {
        this.channelName = channelName;
        this.messages = messages;
    }

    // Getters
    public String getChannelName() { return channelName; }
    public int getMessages() { return messages; }

    // Setters
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setMessages(int messages) { this.messages = messages; }
}
