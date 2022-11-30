package com.example.myapp;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONObject;

/**
 * Class representing any media file (Image or Video)
 */
public class MediaFile implements Serializable{
    private static Random randomEngine = new Random();
    private static final long serialVersionUID = -2723363051271966962L;

    private String name;//Filename
    private long size;//File's size in bytes
    private int width;
    private int height;//Dimensions for either frame or image
    private byte[] chunck = null;

    // Constructor(s)
    public MediaFile(JSONObject obj) { Load(obj); }
    public MediaFile(long size, int width, int height) {
        generateRandomName();
        this.size = size;
        this.width = width;
        this.height = height;
    }

    public MediaFile(String name, long size, int width, int height) {
        this.name = name;
        this.size = size;
        this.width = width;
        this.height = height;
    }

    public MediaFile(String name, long size, int width, int height, byte[] chunck) {
        this.name = name;
        this.size = size;
        this.width = width;
        this.height = height;
        this.chunck = chunck;
    }

    // Getters
    public String getName() { return name; }
    public long getSize() { return size; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public byte[] getChunck() { return chunck; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setSize(long size) { this.size = size; }
    public void setWitdh(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setChunck(byte[] chunck) { this.chunck = chunck; }

    /**
     * Creates a random to be used as indetifier for the file
     * @see https://www.programiz.com/java-programming/examples/generate-random-string
     */
    private void generateRandomName() {

        // create a string of uppercase and lowercase characters and numbers
        String upperAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerAlphabet = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";

        // combine all strings
        String alphaNumeric = upperAlphabet + lowerAlphabet + numbers;

        // create random string builder
        StringBuilder sb = new StringBuilder();

        // create an object of Random class

        // specify length of random string
        int length = 32;

        for(int i = 0; i < length; i++) {

            // generate random index number
            int index = randomEngine.nextInt(alphaNumeric.length());

            // get character specified by index
            // from the string
            char randomChar = alphaNumeric.charAt(index);

            // append the character to string builder
            sb.append(randomChar);
        }

        name =  sb.toString();
    }

    //IO
    public Map<String, Object> export()
    {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("name", name);
        obj.put("size", size);
        obj.put("width", width);
        obj.put("height", height);
        return obj;
    }

    public void Load(JSONObject obj)
    {
        name = (String) obj.get("name");
        size = (Long) obj.get("size");
        width = ((Long)obj.get("width")).intValue();
        height = ((Long)obj.get("height")).intValue();
    }
}
