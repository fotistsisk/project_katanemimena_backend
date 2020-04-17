package com.example.project_katanemimena_2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class ArtistSelectionActivity extends AppCompatActivity implements MyItemClickListener{

    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    MyAdapter mAdapter;
    ArrayList<String> artists;
    String[] songRequest;
    String serverIP ="192.168.1.15";
    private static int[] brokerPorts = {8900,8901,8902};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists);
        artists = new ArrayList<>();
        songRequest = new String[2];

        recyclerView = (RecyclerView) findViewById(R.id.artist_recycler_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL)); //add divider

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        mAdapter = new MyAdapter(artists);
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);

        songRequest[0] = "all_artists";

        new getArtists().execute(songRequest);

    }

    @Override
    public void onItemClick(View view, int position) {
        String artist = artists.get(position);
        if(artists != null){
            Intent myIntent = new Intent(ArtistSelectionActivity.this, SongSelectionActivity.class);
            myIntent.putExtra("artist", artist); //Optional parameters
            ArtistSelectionActivity.this.startActivity(myIntent);
            //Toast.makeText(this,"ARTIST"+artist, Toast.LENGTH_SHORT).show();
        }
    }

    private class getArtists extends AsyncTask<String[], Integer, ArrayList<String>> {

        protected void onProgressUpdate(Integer... progress) {

        }

        protected ArrayList<String> doInBackground(String[]... songRequests) {
            Socket requestSocket = new Socket();
            int brokerPort;
            ArrayList<String> artistsStrings = new ArrayList<>();
            //we reach out to a random broker
            Random r = new Random();
            brokerPort = brokerPorts[r.nextInt(3)];
            //connect to the broker
            try {
                requestSocket = new Socket(serverIP, brokerPort);
                Log.d("LOG CAT:","CONNECTED TO BROKER");
                InputStream inputStream = requestSocket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                // get the port of the broker thread that handle our request
                int threadPort = dataInputStream.readInt();
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
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                artistsStrings.addAll((ArrayList<String>) objectInputStream.readObject());

                requestSocket.close();

                Log.d("LOG CAT:","Artist 1 : " + artistsStrings.get(0));


            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            return artistsStrings;
        }

        protected void onPostExecute(ArrayList<String> result) {
            mAdapter.notifyDataSetChanged(result);
        }
    }
}
