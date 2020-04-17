package com.example.project_katanemimena_2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Random;

public class SongSelectionActivity extends AppCompatActivity implements MyItemClickListener{

    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    MyAdapter mAdapter;
    ArrayList<String> songs;
    int brokerPort;
    byte[] songData;
    URI uri;
    String[] songRequest = new String[2];
    String serverIP ="192.168.1.15";
    private static int[] brokerPorts = {8900,8901,8902};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        songs = new ArrayList<>();

        Intent intent = getIntent();

        recyclerView = (RecyclerView) findViewById(R.id.song_recycler_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL)); //add divider

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        mAdapter = new MyAdapter(songs);
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);

        songRequest[0] = intent.getStringExtra("artist");
        songRequest[1] = "all_songs";

        new getSongs().execute(songRequest);
    }

    @Override
    public void onItemClick(View view, int position) {
        String song = songs.get(position);
        if(song != null){
            //Toast.makeText(this,"SONG "+song, Toast.LENGTH_SHORT).show();
            songRequest[1] = song;
            new getSongData().execute(songRequest);
        }
    }

    private class getSongs extends AsyncTask<String[], Integer, ArrayList<String>> {
        private String[] songRequest;
        private Socket requestSocket;
        private int code,threadPort;
        ArrayList<String> songStrings;
        protected void onProgressUpdate(Integer... progress) {

        }

        protected ArrayList<String> doInBackground(String[]... songRequests) {
            songRequest = songRequests[0];
            requestSocket = new Socket();
            songStrings = new ArrayList<>();
            //we reach out to a random broker
            Random r = new Random();
            brokerPort = brokerPorts[r.nextInt(3)];
            //connect to the broker
            try {
                connectAndSendRequest();
                requestSocket.close();

                Log.d("LOG CAT:","Artist 1 : " + songStrings.get(0));


            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            return songStrings;
        }

        private void connectAndSendRequest() throws IOException, ClassNotFoundException {
            requestSocket = new Socket(serverIP, brokerPort);
            Log.d("LOG CAT:","CONNECTED TO BROKER");
            InputStream inputStream = requestSocket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            // get the port of the broker thread that handle our request
            threadPort = dataInputStream.readInt();
            dataInputStream.close();
            requestSocket.close();
            //connect with the thread
            requestSocket = new Socket(serverIP, threadPort);
            OutputStream outputStream = requestSocket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            //send the request
            objectOutputStream.writeObject(songRequest);
            objectOutputStream.flush(); // send the message
            inputStream = requestSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
            code =  dataInputStream.readInt(); //read code
            if(code == threadPort){
                inputStream = requestSocket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                songStrings.addAll((ArrayList<String>) objectInputStream.readObject());
            }
            else{
                requestSocket.close();
                brokerPort = code;
                connectAndSendRequest();
            }
        }

        protected void onPostExecute(ArrayList<String> result) {
            mAdapter.notifyDataSetChanged(result);
        }
    }

    private class getSongData extends AsyncTask<String[], Integer, byte[]> {
        private String[] songRequest;
        private Socket requestSocket;
        private int code,threadPort;
        private MediaPlayer mediaPlayer = new MediaPlayer();
        byte[] songData;
        protected void onProgressUpdate(Integer... progress) {

        }

        protected byte[] doInBackground(String[]... songRequests) {
            songRequest = songRequests[0];
            requestSocket = new Socket();
            try {
                connectAndSendRequest();
                requestSocket.close();


            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            return songData;
        }

        private byte[] connectAndSendRequest() throws IOException, ClassNotFoundException {
            requestSocket = new Socket(serverIP, brokerPort);
            Log.d("LOG CAT:","CONNECTED TO BROKER");
            InputStream inputStream = requestSocket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            // get the port of the broker thread that handle our request
            threadPort = dataInputStream.readInt();
            dataInputStream.close();
            requestSocket.close();
            //connect with the thread
            requestSocket = new Socket(serverIP, threadPort);
            OutputStream outputStream = requestSocket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            //send the request
            objectOutputStream.writeObject(songRequest);
            objectOutputStream.flush(); // send the message
            inputStream = requestSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
            code =  dataInputStream.readInt(); //read code
            if(code == threadPort){
                int size = dataInputStream.readInt();
                songData = new byte[size];

                Log.d("LOG CAT:","SIZE : " + size);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                byte[] chunk = new byte[512];
                chunk = (byte[])objectInputStream.readObject();
                int i=0;
                //we need to reassemple the array with the chucks to get the full song
                while(i<(size/512)-512){
                    System.arraycopy(chunk,0, songData,i*512,512);
                    chunk = (byte[])objectInputStream.readObject();
                    i++;
                }
                requestSocket.close();
            }
            else{
                requestSocket.close();
                brokerPort = code;
                connectAndSendRequest();
                return null;
            }
            return songData;
        }

        protected void onPostExecute(byte[] result) {
            playMp3(result);
/*            // create temp file that will hold byte array
            File tempMp3 = null;
            try {



                tempMp3 = File.createTempFile("temp", "mp3");
                tempMp3.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempMp3);
                fos.write(result);
                fos.close();
                AssetFileDescriptor afd = getAssets().openFd("temp.mp3");
                MediaPlayer player = new MediaPlayer();
                player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                player.prepare();
                player.start();

            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }

        private void playMp3(byte[] mp3SoundByteArray) {
            try {
                // create temp file that will hold byte array
                File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
                tempMp3.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempMp3);
                fos.write(mp3SoundByteArray);
                fos.close();

                // resetting mediaplayer instance to evade problems
                mediaPlayer.reset();

                // In case you run into issues with threading consider new instance like:
                // MediaPlayer mediaPlayer = new MediaPlayer();

                // Tried passing path directly, but kept getting
                // "Prepare failed.: status=0x1"
                // so using file descriptor instead
                FileInputStream fis = new FileInputStream(tempMp3);
                mediaPlayer.setDataSource(fis.getFD());

                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException ex) {
                String s = ex.toString();
                ex.printStackTrace();
            }
        }
    }
}
