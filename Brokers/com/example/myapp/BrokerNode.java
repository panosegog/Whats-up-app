package com.example.myapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class that handles request on broker on different threads
 */
public class BrokerNode extends Thread {

    // List of meta-data for all channels
    public static HashMap<String, ChannelMeta> ChannelsMeta = new HashMap<>();
    // List of channels that the broker is responsible for 
    public static HashMap<String, Channel> Channels = new HashMap<>();
    
    // Socket that will be used for communication by this thread
    private Socket socket;
    // Object streams
    private ObjectOutputStream out;
    private ObjectInputStream in;

    //Constructor
    public BrokerNode(Socket sock) {
        socket = sock;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Handles request from other brokers
     * @param brokerid Other's broker identifier
     */
    private void handleBroker(final int brokerid) {
        InetSocketAddress broker = new InetSocketAddress(socket.getInetAddress(), Broker.MASTER_PORT + brokerid);
        Broker.Brokers[brokerid] = broker;
        Broker.left -= 1;

        //While not all brokers have join yet sleep for a second
        while(Broker.left > 0)
        {
            try { Thread.sleep(1000); }
            catch (InterruptedException e) {e.printStackTrace(); }
        }

        try { out.writeObject(Broker.Brokers); }
        catch (IOException e) { e.printStackTrace(); }
        finally { if(ChannelsMeta.size() == 0) Broker.LoadChannels(0); }
    }

    /**
     * Handles a request for the list of channels that the user is subscribed to
     * @param username
     */
    private void ConsumerChannels(String username) {
        //Find channels for user
        ArrayList<ChannelMeta> channels = new ArrayList<>();
        for(final ChannelMeta channel: ChannelsMeta.values())
        {
            if(channel.isSubscribed(username))
                channels.add(channel);
        }
        //Send response
        try {
            out.writeObject(channels);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void run() {
        try {
            Message req = (Message)in.readObject();
            switch(req.getType()) {
                case NewBroker://Other broker request
                    handleBroker((Integer)req.getContent());
                    break;
                case UpdateChannel://Other broker request
                {
                    ChannelMeta meta = (ChannelMeta)req.getContent();
                    ChannelsMeta.put(meta.getChannelName(), meta);
                    break;
                }
                case ListBrokers://Client request
                    out.writeObject(Broker.Brokers);
                    break;
                case ListChannels://Consumer request
                    ConsumerChannels(req.getUsername());
                    break;
                case Text://Publisher request
                    PullText(req);
                    break;
                case Image://Publisher request
                case Video://Publisher request
                    PullMedia(req);
                    break;
                case PullMedia://Consumer request
                    PushMedia((MediaFile)req.getContent());
                    break;
                case ChannelHistory://Consumer request
                    PushChannel(req.getChannel(), (Integer)req.getContent());
                    break;
                default:
                    break;
            }
            out.close();
            in.close();
            socket.close();
        }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
        catch(IOException e) { e.printStackTrace(); }
    }

    /**
     * Pulls text from publisher and updates the apropriate channel
     *  both in memory and on in disc
     * @param request Message containing the data that the publisher is "posting"
     */
    private synchronized void PullText(Message request) {
        Channel channel = Channels.get(request.getChannel());
        if(channel == null)//Safety reasons
            return;
        channel.getMessages().add(request);
        channel.Write("../channels");
        
        ChannelMeta meta = ChannelsMeta.get(request.getChannel());
        meta.setMessages(meta.getMessages()+1);
        NotifyOtherBrokers(meta);
    }

    /**
     * Pulls media (Image or Video) and updates the apropriate channel
     * @param request Message containing the data that the publisher is "posting"
     */
    private synchronized void PullMedia(Message request) {
        Channel channel = Channels.get(request.getChannel());
        if(channel == null)//Safety reasons
            return;
        MediaFile media = (MediaFile) request.getContent();
        final String dir = request.getType() == MessageType.Image ? "../pictures" : "../videos";
        final File file = new File(dir, media.getName());
        FileOutputStream fileout = null;
        try {
            fileout = new FileOutputStream(file);
            fileout.write(media.getChunck());
            fileout.flush();
        }
        catch (IOException e) { e.printStackTrace(); }
        try {
            while(true)
            {
                Message data = (Message) in.readObject();
                if(data.getType() == MessageType.Finish)
                    break;
                media = (MediaFile) data.getContent();

                try {
                    fileout.write(media.getChunck());
                    fileout.flush();
                }
                catch (IOException e1) { e1.printStackTrace(); }
            }
            fileout.close();
        }
        catch (ClassNotFoundException | IOException e) { e.printStackTrace(); }
        
        channel.getMessages().add(request);
        channel.Write("../channels");

        ChannelMeta meta = ChannelsMeta.get(request.getChannel());
        meta.setMessages(meta.getMessages()+1);
        NotifyOtherBrokers(meta);
    }

    /**
     * Sends to a consumer the channel history from the requested message and after
     * @param channelName Channels name (aka topic's identifier) 
     * @param msgIndex index of the last message that the consumer is left on
     */
    private synchronized void PushChannel(String channelName, int msgIndex) {
        Channel channel = Channels.get(channelName);
        if(channel == null)
            return;
        ArrayList<Message> messages = channel.getMessages();
        try {
            for(int i = msgIndex; i < messages.size(); i++)
            {
                out.writeObject(messages.get(i));
                out.flush();
            }
            out.writeObject(new Message("server", MessageType.Finish));
            out.flush();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Sends media to the consumer that asked it
     * @param filename Identifier for a media file
     */
    private void PushMedia(MediaFile media)
    {
        final String filename = media.getName();
        //Check if file exists
        final boolean isImage = (new File("../pictures", filename)).exists();
        final boolean isVideo = (new File("../videos", filename)).exists();
        if(!isImage && !isVideo)
            return;
        
        final String dir = isImage ? "../pictures" : "../videos";
        try {
            FileInputStream input = new FileInputStream(new File(dir, filename));
            long offset = 0;
            while(offset < media.getSize())
            {
                final long left = media.getSize() - offset;
                final int size = left < 4096 ? (int)left : 4096;
                byte[] chunck = new byte[size];
                input.read(chunck);
                out.writeObject(new Message(MessageType.PullMedia, chunck));
                out.flush();
                offset += size;
            }
            input.close();

            out.writeObject(new Message(MessageType.Finish, null));
            out.flush();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Sends metadata to all other Brokers
     * @param meta Channel's metadata
     */
    private void NotifyOtherBrokers(ChannelMeta meta) {
        for(InetSocketAddress broker: Broker.Brokers)
        {
            if(broker.getPort() == socket.getLocalPort())//This is me not need to infrom me
                continue;
            try {
                Socket client = new Socket(broker.getHostName(), broker.getPort());
                ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(client.getInputStream());
                
                output.writeObject(new Message(MessageType.UpdateChannel, meta));
                
                output.close();
                input.close();
                client.close();
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    }
}
