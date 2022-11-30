package com.example.myapp;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Class holding information about a conversation betweeen subscribers
 */
public class Channel{

    private String name;//Channel(Topic)'s name
    private ArrayList<Message> messages = new ArrayList<>();//Messages send in channel to be used as a queue
    private ArrayList<String> subscribers = new ArrayList<>();//Subscribers to this channel

    //Constructor(s)
    public Channel(String topicname) { name = topicname; }
    public Channel(String topicname, ArrayList<String> subs) {
        name = topicname;
        subscribers = subs;
    }

    public Channel(String topicname, ArrayList<Message> msgs, ArrayList<String> subs) {
        name = topicname;
        messages = msgs;
        subscribers = subs;
    }

    //Getters
    public String getName() { return name; }
    public ArrayList<Message> getMessages() { return messages; }
    public ArrayList<String> getSubscribers() { return subscribers; }

    //Setters
    public void setName(String topicname) { name = topicname; }
    public void setMessages(ArrayList<Message> msgs) { messages = msgs; }
    public void setSubscribers(ArrayList<String> subs) { subscribers = subs; }

    //File IO
    public void Write(final String directory) {
        Map<String, Object> channel = new LinkedHashMap<>();

        ArrayList<String> subs = new ArrayList<>();
        for(String sub: subscribers)
            subs.add(sub);

        ArrayList<Map<String, Object>> msgs = new ArrayList<>();
        for(Message msg: messages)
            msgs.add(msg.export());

        channel.put("subscribers", subs);
        channel.put("messages", msgs);
        channel.put("name", name);

        final String filepath = directory + "/" + name;
        final String output = JSONValue.toJSONString(channel);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath));
            bw.write(output);
            bw.flush();
            bw.close();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void Load(final String filepath) {
        subscribers = new ArrayList<>();
        messages = new ArrayList<>();

        try {
            JSONObject obj = (JSONObject) JSONValue.parse(new FileReader(filepath));
            name = (String) obj.get("name");
            JSONArray subs = (JSONArray) obj.get("subscribers");
            for(Object sub: subs)
                subscribers.add((String)sub);
            JSONArray msgs = (JSONArray) obj.get("messages");
            for(Object msg: msgs)
                messages.add(new Message((JSONObject)msg));

        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
    }

    /**
     * Produces the md5 hash for the channel
     * @see <a href="https://www.geeksforgeeks.org/md5-hash-in-java/">Code from (Modified)</a>
     */
    public int getHash() {
        try {

            // Static getInstance method is called with hashing MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // digest() method is called to calculate message digest
            //  of an input digest() return array of byte
            byte[] messageDigest = md.digest(name.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32)
                hashtext = "0" + hashtext;
            return Integer.parseInt(hashtext.substring(0, 5), 16);
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if(other==this)
            return true;
        if(!(other instanceof Channel))
            return false;
        Channel meta = (Channel) other;
        return meta.getName().compareTo(name) == 0;
    }

}
