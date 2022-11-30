package com.example.myapp;

/**
 * Enumaration for different kind of messages use in order
 *  to interpret the Message's content correctly
 */
public enum MessageType {
    NewBroker,
    UpdateChannel,
    ListBrokers,
    ListChannels,
    ChannelHistory,
    PullMedia,
    Text,
    Image,
    Video,
    Finish,
}
