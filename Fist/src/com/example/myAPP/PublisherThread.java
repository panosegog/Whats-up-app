package com.example.myAPP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PublisherThread extends Thread {
    UserNode app;
    Socket client;
    ObjectInputStream in;
    ObjectOutputStream out;
    public PublisherThread(UserNode app, Socket client){
        this.app=app;
        this.client=client;
        try {
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            Message msg;
            msg = (Message) in.readObject();
            if (msg.channelName.equals("Server")) {
                int counter = app.channelName.hashtagsPublished.size();

                for (int i = 0; i < app.channelName.hashtagsPublished.size(); i++) {
                    msg = new Message(app.channelName.channelName, app.channelName.hashtagsPublished.get(i), String.valueOf(counter), null);
                    out.writeObject(msg);
                    out.flush();
                    counter--;
                }
                if (counter == 0) {
                    msg = new Message(app.channelName.channelName, null, "-1", null);
                    out.writeObject(msg);
                    out.flush();
                }

            } else {
                msg = new Message(app.channelName.channelName, null, null, null);
                out.writeObject(msg);
                out.flush();
            }
            while (true) {
                msg = (Message) in.readObject();
                app.push(msg.getKey(), null);
            }
        } catch (IOException | ClassNotFoundException e) {

            try {
                in.close();
                out.close();
                client.close();

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
