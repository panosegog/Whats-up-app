package com.example.myapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.jpeg.JpegParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class Tester {

    public static void main(String[] args)
    {
        /*ArrayList<ChannelMeta> channels = null;
        try (Socket sock = new Socket(Broker.MASTER_IP, Broker.MASTER_PORT))
        {
            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

            out.writeObject(new Message("Panos", MessageType.ListChannels));
            channels = (ArrayList<ChannelMeta>) in.readObject();
            out.close();
            in.close();
            sock.close();
        }
        catch (IOException e) { e.printStackTrace(); }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
        finally
        {
            if(channels == null)
                return;

            System.out.println('[');
            for(ChannelMeta channel: channels)
            {
                System.out.println("\t{");
                System.out.println("\t\tname: "+ channel.getChannelName());
                System.out.println("\t\tbroker: "+ channel.getBroker().toString());
                System.out.println("\t\tmessages: "+ channel.getMessages());
                System.out.print("\t\t[");
                for(String sub: channel.getSubscribers())
                    System.out.print(sub+", ");
                System.out.println(']');
                System.out.println("\t},");
            }
            System.out.println(']');
        }*/

        final File file = new File("pictures/sample.jpeg");
        MediaFile media = null;
        try {
            BodyContentHandler handler = new BodyContentHandler();
            FileInputStream inputStream = new FileInputStream(file);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            JpegParser parser = new JpegParser();
            parser.parse(inputStream, handler, metadata, context);
            inputStream.close();

            final String str_size = metadata.get("File Size");
            final String str_width = metadata.get("Image Width");
            final String str_height = metadata.get("Image Width");

            final long size = Long.parseLong(str_size.substring(0, str_size.indexOf(' ')));
            final int width = Integer.parseInt(str_width.substring(0, str_width.indexOf(' ')));
            final int height = Integer.parseInt(str_height.substring(0, str_height.indexOf(' ')));
            
            media = new MediaFile(size, width, height);
        }
        catch (IOException | SAXException | TikaException e) { e.printStackTrace(); }
        
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            socket = new Socket("192.168.1.4", 62900);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new Message("Panos", MessageType.ListChannels));
            ArrayList<ChannelMeta> channels = (ArrayList<ChannelMeta>)in.readObject();
            for(ChannelMeta channel: channels)
                System.out.println(channel.getChannelName() + "\t(" + channel.getMessages() + ")");

            out.close();
            in.close();
            socket.close();    
            
            InetSocketAddress broker = channels.get(1).getBroker();
            System.out.println(broker);
            socket = new Socket(broker.getHostName(), broker.getPort());
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }
        catch (UnknownHostException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
        catch (ClassNotFoundException e) { e.printStackTrace(); }

        try {
            FileInputStream inputStream = new FileInputStream(file);
            long offset = 0;
            while(offset < media.getSize())
            {
                final long left = media.getSize() - offset;
                final int size = left < 4096 ? (int)left : 4096;
                byte[] chunck = new byte[size];
                inputStream.read(chunck);
                media = new MediaFile(media.getName(), media.getSize(), media.getWidth(), media.getHeight(), chunck);
                System.out.println(offset+"/"+media.getSize());
                out.writeObject(new Message("Panos", "Baneas-Panos", MessageType.Image, media));
                out.flush();
                offset += size;
            }
            inputStream.close();
            out.writeObject(new Message("Panos", MessageType.Finish));
            out.flush();

            out.close();
            in.close();
            socket.close();
        }
        catch (IOException e) { e.printStackTrace(); }
        
    }
    
}
