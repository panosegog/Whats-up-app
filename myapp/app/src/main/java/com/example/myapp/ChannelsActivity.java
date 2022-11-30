package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ChannelsActivity extends AppCompatActivity {

    public static final String MASTER_IP = "192.168.1.4";
    public static final int MASTER_PORT = 62900;

    private ChannelsAdapter adapter;
    private InetSocketAddress[] brokers = null;
    private String username;
    private Handler handler;
    private volatile boolean running;
    private Context context;
    private ArrayList<ChannelMeta> stored;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels);

        context = this;
        Intent intent = getIntent();
        username = intent.getStringExtra("username");

        adapter = new ChannelsAdapter(this);
        ListView listView = findViewById(R.id.listViewChannels);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChannelMeta item = adapter.getItem(position);
                Intent intent = new Intent(ChannelsActivity.this, ChannelHistoryActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("meta", item);
                running = false;
                startActivity(intent);
                finish();
            }
        });

        handler = new ChannelUpdate(Looper.myLooper());
        Start();

    }

    private class ChannelUpdate extends Handler {

        public ChannelUpdate(@NonNull Looper looper) { super(looper); }

        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            ArrayList<ChannelMeta> channels = (ArrayList<ChannelMeta>) msg.obj;
            adapter.setChannels(channels);
        }
    }

    private void Start()
    {
        running = true;
        (new Thread(new Runnable() {
            @Override
            public void run() {
                //Load channels metadata from disc
                stored = new ArrayList<>();
                File userDir = new File(context.getExternalFilesDir(null), username);
                for(File fileEntry: userDir.listFiles())
                {
                    if(fileEntry.isDirectory())
                        continue;
                    Channel channel = new Channel(fileEntry.getName());
                    channel.Load(fileEntry.getPath());
                    stored.add(new ChannelMeta(channel.getName(), null, channel.getMessages().size(), new ArrayList<>()));
                }
                try {
                    Socket client = new Socket(MASTER_IP, MASTER_PORT);
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                    out.writeObject(new Message(MessageType.ListBrokers, null));
                    brokers = (InetSocketAddress[])in.readObject();

                    out.close();
                    in.close();
                    client.close();


                    while(running) {
                        InetSocketAddress broker = brokers[ThreadLocalRandom.current().nextInt(brokers.length)];

                        client = new Socket(broker.getHostName(), broker.getPort());
                        out = new ObjectOutputStream(client.getOutputStream());
                        in = new ObjectInputStream(client.getInputStream());

                        out.writeObject(new Message(username, MessageType.ListChannels));
                        ArrayList<ChannelMeta> channels = (ArrayList<ChannelMeta>) in.readObject();
                        for(ChannelMeta channelMeta: channels)
                        {
                            //Log.d("MemoryCheck", channel.getChannelName()+": "+channel.getMessages());
                            final int index = stored.indexOf(channelMeta);
                            if(index == -1)//New channel (Not stored on locally yet)
                                continue;

                            ChannelMeta meta = stored.get(index);
                            channelMeta.setMessages(channelMeta.getMessages()-meta.getMessages());

                        }
                        out.close();
                        in.close();
                        client.close();

                        android.os.Message msg = new android.os.Message();
                        msg.obj = channels;
                        handler.sendMessage(msg);

                        try {
                            Thread.sleep(5000);
                        }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
                catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
            }
        })).start();
    }

}