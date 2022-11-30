package com.example.myapp;

import java.io.Serializable;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONObject;

/**
 * Class that represents all kind of messages
 *  in networking
 */
public class Message implements Serializable{
    //Class serialization ID
    private static final long serialVersionUID = -2723363051271966964L;
    //Fields
    private String channel = null;//Topic's identifier
    private String username = null;//Sender's identifier
    private MessageType type;
    private Object content;//Actual data

    // Constructor(s)
    Message(JSONObject obj) { load(obj); }
    Message(String user, MessageType type) {
        username = user;
        this.type = type;
    }

    Message(MessageType type, Object data) {
        this.type = type;
        content = data;
    }
    
    Message(String user, String channel, MessageType type, Object data) {
        username = user;
        this.channel = channel;
        this.type = type;
        content = data;
    }


    // Getters
    public String getChannel() { return channel; }
    public Object getContent() { return content; }
    public MessageType getType() { return type; }
    public String getUsername() { return username; }

    // Setters 
    public void setChannel(String channel) { this.channel = channel; }
    public void setContent(Object content) { this.content = content; }
    public void setType(MessageType type) { this.type = type; }
    public void setUsername(String username) { this.username = username; }

    // IO
    public Map<String, Object> export() {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("type", type.toString());
        obj.put("user", username);
        switch(type)
        {
            case Text:
                obj.put("content", (String)content);
                break;
            case Image:
            case Video:
                obj.put("content", ((MediaFile)content).export());
                break;
            default://Maybe through exception instead
                return null;
        }
        return obj;
    }
    
    public void load(JSONObject obj) {
        username = (String)obj.get("user");
        type = MessageType.valueOf((String) obj.get("type"));
        switch(type)
        {
            case Text:
                content = (String)obj.get("content");
                break;
            case Image:
            case Video:
                content = new MediaFile((JSONObject)obj.get("content"));
                break;
            default:
                return;
        }
    }
    
}
