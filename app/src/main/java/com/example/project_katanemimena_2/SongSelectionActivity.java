package com.example.project_katanemimena_2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
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

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private MyAdapter mAdapter;
    private ArrayList<String> songs;
    private int brokerPort;
    private byte[] songData;
    private URI uri;
    private String[] songRequest = new String[2];
    private String serverIP ="192.168.1.15";
    private static int[] brokerPorts = {8900,8901,8902};
    private MediaPlayer mediaPlayer;
    //Used to pause/resume MediaPlayer
    private int resumePosition;

    private ProgressDialog dialogSongs;
    private ProgressDialog dialogData;

    private getSongs asyncTaskGetSong;
    private getSongData asyncTaskGetData;

    private ImageButton ib_play_pause;


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

        initMediaPlayer();

        dialogSongs = new ProgressDialog(SongSelectionActivity.this);

        ib_play_pause = (ImageButton) findViewById(R.id.play_pause_button);
        ib_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()){
                    pauseMedia();
                    ib_play_pause.setImageResource(R.drawable.play_arrow);
                }
                else{
                    new getSongData().execute(songRequest);
                    ib_play_pause.setImageResource(R.drawable.pause);
                }
            }
        });

        final ImageButton ib_stop = (ImageButton) findViewById(R.id.stop_button);
        ib_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ib_play_pause.setImageResource(R.drawable.play_arrow);
                stopMedia();
            }
        });

        asyncTaskGetSong = new getSongs();
        asyncTaskGetSong.execute(songRequest);
    }

    @Override
    public void onItemClick(View view, int position) {
        String song = songs.get(position);
        if(song != null){
            //Toast.makeText(this,"SONG "+song, Toast.LENGTH_SHORT).show();
            songRequest[1] = song;
            ib_play_pause.setImageResource(R.drawable.play_arrow);
        }
    }

    private class getSongs extends AsyncTask<String[], Integer, ArrayList<String>> {
        private String[] songRequestAsync;
        private Socket requestSocket;
        private int code,threadPort;
        ArrayList<String> songStrings;
        private CountDownTimer countDownTimer;

        @Override
        protected void onPreExecute() {
            dialogSongs.setMessage("Getting songs, please wait.");
            dialogSongs.show();
            countDownTimer = new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    asyncTaskGetSong.cancel(true);
                    if(dialogSongs.isShowing())
                        dialogSongs.cancel();
                    new AlertDialog.Builder(SongSelectionActivity.this)
                            .setTitle("Error")
                            .setMessage("Error retrieving songs.\nDo you want to try again?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    asyncTaskGetSong = new getSongs();
                                    asyncTaskGetSong.execute(songRequestAsync);
                                }
                            })
                            .setNegativeButton("NO", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                }
            }.start();
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected ArrayList<String> doInBackground(String[]... songRequests) {
            songRequestAsync = songRequests[0];
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
            objectOutputStream.writeObject(songRequestAsync);
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
            dialogSongs.dismiss();
            dialogSongs=null;
            countDownTimer.cancel();
            mAdapter.notifyDataSetChanged(result);
        }
    }

    private class getSongData extends AsyncTask<String[], Integer, byte[]> {
        private String[] songRequestAsync;
        private Socket requestSocket;
        private int code,threadPort;
        byte[] songData;
        CountDownTimer countDownTimer;

        protected void onProgressUpdate(Integer... progress) {

        }

        @Override
        protected void onPreExecute() {
            dialogData = new ProgressDialog(SongSelectionActivity.this);
            dialogData.setMessage("Getting song data, please wait.");
            dialogData.show();
            countDownTimer = new CountDownTimer(10000, 1000) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    asyncTaskGetData.cancel(true);
                    if(dialogData.isShowing())
                        dialogData.cancel();
                    new AlertDialog.Builder(SongSelectionActivity.this)
                            .setTitle("Error")
                            .setMessage("Error retrieving songData.\nDo you want to try again?")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    asyncTaskGetData = new getSongData();
                                    asyncTaskGetData.execute(songRequestAsync);
                                }
                            })
                            .setNegativeButton("NO", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                }
            }.start();
        }

        protected byte[] doInBackground(String[]... songRequests) {
            songRequestAsync = songRequests[0];
            requestSocket = new Socket();
            try {
                if(checkDownload(songRequestAsync[1])){

                }
                else
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
            objectOutputStream.writeObject(songRequestAsync);
            objectOutputStream.flush(); // send the message
            inputStream = requestSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
            code =  dataInputStream.readInt(); //read code
            if(code == threadPort){
                int packets = dataInputStream.readInt();
                songData = new byte[packets*512];

                Log.d("LOG CAT:","PACKETS : " + packets);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                byte[] chunk = new byte[512];
                int i=1;
                //we need to reassemple the array with the chucks to get the full song
                do{
                    Log.d("LOG CAT:","TIMES : " + i);
                    chunk = (byte[])objectInputStream.readObject();
                    System.arraycopy(chunk,0, songData,(i-1)*512,512);
                    i++;
                } while(i<packets);
                Log.d("LOG CAT:","TIMES : " + i);
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
            dialogData.dismiss();
            countDownTimer.cancel();
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

        private boolean checkDownload(String song){
            return false;
        }

        private void playMp3(byte[] mp3SoundByteArray) {
            try {
                // create temp file that will hold byte array
                File tempMp3 = File.createTempFile("temp", "mp3", getCacheDir());
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
                playMedia();
            } catch (IOException ex) {
                String s = ex.toString();
                ex.printStackTrace();
            }
        }
    }
    //mediaPlayer

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        stopMedia();
        mediaPlayer.release();
        mediaPlayer = null;
        super.onDestroy();
    }
}
