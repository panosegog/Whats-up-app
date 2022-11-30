package com.example.myapp;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * Class used for communication between Broker and Consumer
 *  It's basically holds only the meta-data of a channel
 */
public class ChannelMeta implements Serializable {
    //Class serialization ID
    private static final long serialVersionUID = 7710439065836944524L;
    // Fields
    private String channelName = "";
    private InetSocketAddress broker = null;
    private int messages = 0;
    private ArrayList<String> subscribers;

    // Constructor(s)
    public ChannelMeta(String channelName) { this.channelName = channelName; }
    public ChannelMeta(String channelName, InetSocketAddress broker, int messages, ArrayList<String> subs) {
        this.channelName = channelName;
        this.broker = broker;
        this.messages = messages;
        subscribers = subs;
    }

    /**
     * Checks whether a specified is subscribed to this channel or not
     * @param user Sting containing the username to be checked
     * @return true if the user is subcribed, false otherwise
     */
    public boolean isSubscribed(String user) {
        for(String sub: subscribers)
        {
            if(sub.compareTo(user) == 0)
                return true;
        }
        return false;
    }

    // Getters
    public String getChannelName() { return channelName; }
    public InetSocketAddress getBroker() { return broker; }
    public int getMessages() { return messages; }
    public ArrayList<String> getSubscribers() { return subscribers; }

    // Setters
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setBroker(InetSocketAddress broker) { this.broker = broker; }
    public void setMessages(int messages) { this.messages = messages; }
    public void setSubscribers(ArrayList<String> subs) { this.subscribers = subs; }
}