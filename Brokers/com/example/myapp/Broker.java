package com.example.myapp;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Entry point for broker procceses
 */
public class Broker {

    //IP & port of the first broker ("Master") should be know
    //  to everybody consumer and brokers alike
    public static final String MASTER_IP = "192.168.1.4";
    public static final int MASTER_PORT = 62900; 

    //All currently working brokers
    public static InetSocketAddress[] Brokers = null;
    //Number of brokers that we are waiting to connect
    public static volatile int left = 0;

    public static void main(String[] args)
    {
        if(args.length != 2)
        {
            System.err.println("Please specify broker's id as well as the amount of brokers.");
            System.exit(1);
        }

        final int id = Integer.parseInt(args[0]);
        final int brokers = Integer.parseInt(args[1]);

        Brokers = new InetSocketAddress[brokers];
        Brokers[0] = new InetSocketAddress(MASTER_IP, MASTER_PORT);
        left = brokers - 1;

        try {
            ServerSocket server = new ServerSocket(MASTER_PORT + id);
            if(id != 0)
                NotifyMaster(id);
            while(true)
            {
                Socket connection = server.accept();
                BrokerNode thread = new BrokerNode(connection);
                thread.start();
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Notifies "Master" broker that another broker join our force
     *  and wait for the full list of all brokers
     * @param id Broker's identifier (aka index on the Brokers array)
     */
    private static void NotifyMaster(int id)
    {
        try {
            Socket client = new Socket(MASTER_IP, MASTER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            
            out.writeObject(new Message(MessageType.NewBroker, new Integer(id)));
            Brokers = (InetSocketAddress[]) in.readObject();
            
            out.close();
            in.close();
            client.close();
        }
        catch (IOException e) { e.printStackTrace(); }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
        finally { LoadChannels(id); }
    }

    /**
     * Loads all the channels that the broker is responsible for
     * @param id Broker's identifier (aka index on Brokers table)
     */
    public static void LoadChannels(int id)
    {
        File channelsDir = new File("../channels");
        for(File file: channelsDir.listFiles())
        {
            final String channelName = file.getName();
            Channel channel = new Channel(channelName);
            channel.Load(file.getPath());

            final int brokerid = channel.getHash() % Brokers.length;

            InetSocketAddress broker = Brokers[brokerid];
            BrokerNode.ChannelsMeta.put(channelName, new ChannelMeta(channelName, broker, channel.getMessages().size(), channel.getSubscribers()));

            if(brokerid == id)
                BrokerNode.Channels.put(channelName, channel);
        }
    }

}