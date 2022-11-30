package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.jpeg.JpegParser;
import org.apache.tika.parser.mp4.MP4Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ChannelHistoryActivity extends AppCompatActivity {

    private String username;
    private ChannelMeta meta;
    private MessageAdapter adapter;
    private Handler messageHandler;
    private Handler progressHandler;
    private ProgressBar sendingFile;
    private Dialog dialog;
    private Channel channel;
    private Context context;
    private volatile boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_history);

        context = this;
        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        meta = (ChannelMeta) intent.getSerializableExtra("meta");

        EditText editText = findViewById(R.id.editTextMessage);
        ImageView imageView = findViewById(R.id.imageViewSendButton);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String content = editText.getText().toString();
                if(content.length() == 0)
                    Toast.makeText(ChannelHistoryActivity.this, "You must type something as a message", Toast.LENGTH_SHORT).show();
                editText.setText("");
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Socket client = null;
                        try {
                            client = new Socket(meta.getBroker().getHostName(), meta.getBroker().getPort());
                            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                            out.writeObject(new Message(username, meta.getChannelName(), MessageType.Text, content));
                            out.close();
                            in.close();
                            client.close();
                        }
                        catch (IOException e) { e.printStackTrace(); }

                    }
                })).start();
            }
        });

        ImageView filepick = (ImageView) findViewById(R.id.imageViewPickMedia);
        filepick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                final String[] mimeTypes = {"image/*", "video/*"};
                chooseFile.setType("*/*");
                chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                chooseFile = Intent.createChooser(chooseFile, "Pick a media file");
                startActivityForResult(chooseFile, 1);
            }
        });

        adapter = new MessageAdapter(this, username);
        ListView listView = findViewById(R.id.listViewMessages);
        listView.setAdapter(adapter);

        messageHandler = new UpdateHistory(Looper.myLooper());
        progressHandler = new UpdateProgress(Looper.myLooper());
        Start();

    }

    /**
     * Class for handling communication with background threads
     */
    private class UpdateHistory extends Handler {

        public UpdateHistory(@NonNull Looper looper) { super(looper); }

        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            Message message = (Message) msg.obj;
            if(message.getType() == MessageType.Finish)//Finish pulling a media file
            {
                adapter.notifyDataSetChanged();
                return;
            }
            adapter.appendMessage(message);
            if(message.getType() == MessageType.Image || message.getType() == MessageType.Video)
                PullMedia(message);
        }

    }

    /**
     * Class for handling sending progress updates
     */
    private class UpdateProgress extends Handler{
        public  UpdateProgress(@NonNull Looper looper) { super(looper); }

        @Override
        public void handleMessage(@NonNull android.os.Message msg){
            final int percentage = (Integer) msg.obj;
            sendingFile.setProgress(percentage, true);
            if(percentage == 100)
                dialog.dismiss();
        }
    }

    /**
     * Start pulling data from the appropriate broker
     *  using another thread
     */
    private void Start()
    {
        running = true;
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File userDir = new File(context.getExternalFilesDir(null), username);
                    File file = new File(userDir, meta.getChannelName());
                    channel = new Channel(meta.getChannelName());
                    if(file.exists()) {
                        channel.Load(file.getPath());
                        adapter.setMessages(channel.getMessages());
                    }
                    while(running) {
                        //Establish connection with broker
                        Socket client = new Socket(meta.getBroker().getHostName(), meta.getBroker().getPort());
                        ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                        //Ask for any message after the last one we have
                        out.writeObject(new Message(username, meta.getChannelName(), MessageType.ChannelHistory, Integer.valueOf(adapter.getCount())));
                        while (true) {//Get new messages
                            Message message = (Message) in.readObject();
                            if (message.getType() == MessageType.Finish)
                                break;
                            //Save channel data locally
                            channel.getMessages().add(message);
                            channel.Write(userDir.getPath());

                            //Inform Main thread about new message
                            android.os.Message msg = new android.os.Message();
                            msg.obj = message;
                            messageHandler.sendMessage(msg);
                        }

                        //Close connection
                        out.close();
                        in.close();
                        client.close();

                        //Wait for a second before we ask again
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
                catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
            }
        })).start();
    }

    /**
     * Start pulling media file from the appropriate broker
     *  using another thread
     */
    private void PullMedia(Message message)
    {
        MediaFile media = (MediaFile) message.getContent();
        final String extension = message.getType() == MessageType.Image ? ".jpg" : ".mp4";
        final String dir = message.getType() == MessageType.Image ? "/pictures/" : "/videos/";
        final Context context = this;
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Establish connections with the appropriate broker
                    Socket client = new Socket(meta.getBroker().getHostName(), meta.getBroker().getPort());
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                    //Open FileStream
                    File file = new File(context.getExternalFilesDir(null),username+dir+media.getName()+extension);
                    FileOutputStream output = new FileOutputStream(file);

                    //Ask for media-file
                    out.writeObject(new Message(MessageType.PullMedia, media));
                    out.flush();
                    while (true) {//Pull data in chuncks
                        Message data = (Message) in.readObject();
                        if(data.getType() == MessageType.Finish)
                            break;
                        byte[] chunck = (byte[]) data.getContent();
                        output.write(chunck);
                        output.flush();
                    }

                    //Have to close file before inform main thread and try to read
                    output.close();
                    //Inform main thread that we pull file
                    android.os.Message msg = new android.os.Message();
                    msg.obj = new Message(MessageType.Finish, null);
                    messageHandler.sendMessage(msg);

                    //Close connection with broker
                    out.close();
                    in.close();
                    client.close();
                }
                catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
            }
        })).start();
    }

    @Override
    public void onActivityResult(int reqCode, int result, Intent data)
    {
        super.onActivityResult(reqCode, result, data);
        if(reqCode != 1 || result != RESULT_OK)
            return;

        Uri uri = data.getData();
        if(uri == null)
            return;

        final String path = RealPathUtil.getPath(this, uri);

        InputStream input = null;
        MediaFile media = null;
        MessageType type = null;
        try {//Read metadata for file

            InputStream inputStream = this.getContentResolver().openInputStream(uri);
            final long size = (new File(path)).length();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            String str_width = "";
            String str_height = "";
            int width = 0;
            int height = 0;
            if(uri.getPath().contains("image")) {
                JpegParser parser = new JpegParser();
                parser.parse(inputStream, handler, metadata, context);
                type = MessageType.Image;
                str_width = metadata.get("Image Width");
                str_height = metadata.get("Image Height");
                width = Integer.parseInt(str_width.substring(0, str_width.indexOf(' ')));
                height = Integer.parseInt(str_height.substring(0, str_height.indexOf(' ')));
            }
            else{//Video
                MP4Parser parser = new MP4Parser();
                parser.parse(inputStream, handler, metadata, context);
                type = MessageType.Video;
                str_width = metadata.get("tiff:ImageWidth");
                str_height = metadata.get("tiff:ImageLength");
                width = Integer.parseInt(str_width);
                height = Integer.parseInt(str_height);
            }
            inputStream.close();

            media = new MediaFile(size, width, height);
            input = this.getContentResolver().openInputStream(uri);
        }
        catch (IOException | TikaException | SAXException e) { e.printStackTrace(); }

        final MediaFile finalMedia = media;
        final MessageType finalType = type;
        InputStream finalInput = input;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpawnDialog(finalMedia.getName());
            }
        });

        (new Thread(new Runnable() {//Start a thread to publish the media file
            @Override
            public void run() {
                try {
                    Socket client = new Socket(meta.getBroker().getHostName(), meta.getBroker().getPort());
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(client.getInputStream());

                    long offset = 0;
                    while(offset < finalMedia.getSize())
                    {
                        final long left = finalMedia.getSize() - offset;
                        final int size = left < 4096 ? (int)left : 4096;
                        byte[] chunk = new byte[size];
                        finalInput.read(chunk);
                        MediaFile content = new MediaFile(finalMedia.getName(), finalMedia.getSize(), finalMedia.getWidth(), finalMedia.getHeight(), chunk);
                        out.writeObject(new Message(username, meta.getChannelName(), finalType, content));
                        out.flush();
                        offset += size;

                        final int progress = (int)((float)offset/finalMedia.getSize() * 100);
                        android.os.Message msg = new android.os.Message();
                        msg.obj = Integer.valueOf(progress);
                        progressHandler.sendMessage(msg);
                    }
                    finalInput.close();

                    out.writeObject(new Message(username, MessageType.Finish));
                    out.flush();

                    out.close();
                    in.close();
                    client.close();
                }
                catch (IOException e) { e.printStackTrace(); }
            }
        })).start();
    }

    private void SpawnDialog(String medianame)
    {
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.send_dialog);
        final TextView filename = dialog.findViewById(R.id.textViewMediaFile);
        filename.setText(medianame);
        sendingFile = dialog.findViewById(R.id.progressBarSending);
        sendingFile.setProgress(0);

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        running = false;
        Intent intent = new Intent(ChannelHistoryActivity.this, ChannelsActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

}