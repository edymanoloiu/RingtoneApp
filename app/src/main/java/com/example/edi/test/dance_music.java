package com.example.edi.test;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class dance_music extends ListActivity {

    private static int count = 0;
    private MediaPlayer mPlayer = null;
    private TreeMap<String, Integer> songsIDs = new TreeMap<>();
    private String selectedSong = "";
    private float x1, x2;
    private int lastPosition = -1;
    private TreeMap<String, String> songTitles = new TreeMap<>();
    private TreeMap<String, String> songEquivalent = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dance_music);

        ParseSongInfoFile();

        // loop dynamically through (reflection)
        ArrayList<String> music = new ArrayList<>();
        R.raw sondDirectory = new R.raw();
        final Class<R.raw> c = R.raw.class;
        final Field[] fields = c.getDeclaredFields();

        for (int i = 0, max = fields.length; i < max; i++) {
            int resourceId;
            try {
                resourceId = fields[i].getInt(sondDirectory);
                if (!String.valueOf(fields[i].getName()).equals("songtitles")) {
                    music.add(songTitles.get(String.valueOf(fields[i].getName())));
                    songsIDs.put(fields[i].getName(), resourceId);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        //populate listview with song in the app
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, music);
        setListAdapter(adapter);

        //set swipe trigger for listview
        getListView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        float deltaX = x2 - x1;
                        if (deltaX > 5) {
                            //get song title
                            Random r = new Random();
                            ListView mChatListView = getListView();
                            int position = mChatListView.pointToPosition(Math.min(getWindowManager().getDefaultDisplay().getWidth(), (int) event.getX()), (int) event.getY());
                            mChatListView.getChildAt(position).setBackgroundColor(0xFF7DB275);
                            String selectedSongTitle = String.valueOf(mChatListView.getItemAtPosition(position));
                            String randomName = selectedSongTitle + r.nextInt(2015);

                            //write song on to the sdcard or internal memory
                            File newSoundFile;
                            String directoryName;
                            directoryName = Environment.getExternalStorageDirectory().getAbsolutePath();

                            newSoundFile = new File(directoryName, randomName + ".mp3");

                            if (newSoundFile.exists())
                                newSoundFile.delete();

                            Uri mUri = Uri.parse("android.resource://com.example.edi.test/" + songsIDs.get(songEquivalent.get(selectedSongTitle)));
                            ContentResolver mCr = getContentResolver();
                            AssetFileDescriptor soundFile;
                            try {
                                soundFile = mCr.openAssetFileDescriptor(mUri, "r");
                            } catch (FileNotFoundException e) {
                                soundFile = null;
                            }

                            try {
                                byte[] readData = new byte[1024];
                                FileInputStream fis = soundFile.createInputStream();
                                FileOutputStream fos = new FileOutputStream(newSoundFile);
                                int i = fis.read(readData);

                                while (i != -1) {
                                    fos.write(readData, 0, i);
                                    i = fis.read(readData);
                                }
                                fos.close();
                                fis.close();
                            } catch (IOException io) {
                            }

                            //set the song as ringtone
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.MediaColumns.DATA, newSoundFile.getAbsolutePath());
                            values.put(MediaStore.MediaColumns.TITLE, randomName);
                            values.put(MediaStore.MediaColumns.SIZE, newSoundFile.length());
                            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
                            values.put(MediaStore.Audio.Media.ARTIST, R.string.app_name);
                            values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                            values.put(MediaStore.Audio.Media.IS_ALARM, false);
                            values.put(MediaStore.Audio.Media.IS_MUSIC, true);

                            //File file = new File(newSoundFile.getPath());
                            //if (file.exists())
                            //   Toast.makeText(getListView().getContext(), newSoundFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

                            //Insert it into the database
                            Uri uri = MediaStore.Audio.Media.getContentUriForPath(newSoundFile.getAbsolutePath());
                            Uri newUri = getListView().getContext().getContentResolver().insert(uri, values);

                            RingtoneManager.setActualDefaultRingtoneUri(getListView().getContext(), RingtoneManager.TYPE_RINGTONE, newUri);

                            Toast.makeText(getListView().getContext(), "ringtone added successfuly", Toast.LENGTH_SHORT).show();
                        } else if (Math.abs(deltaX) < 5) {
                            ListView mChatListView = getListView();
                            int position = mChatListView.pointToPosition((int) event.getX(), (int) event.getY());
                            if (position != ListView.INVALID_POSITION) {
                                mChatListView.performItemClick(mChatListView.getChildAt(position - mChatListView.getFirstVisiblePosition()), position, mChatListView.getItemIdAtPosition(position));
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        //get text of selected value

        l.getChildAt(position).setBackgroundColor(Color.TRANSPARENT);
        if (lastPosition != -1)
            l.getChildAt(lastPosition).setBackgroundColor(Color.TRANSPARENT);
        String selectedRowFromList = String.valueOf(l.getItemAtPosition(position));

        //make sure the song is correct
        if (mPlayer == null) {
            mPlayer = MediaPlayer.create(this, songsIDs.get(songEquivalent.get(selectedRowFromList)));
        } else if (!selectedSong.equals(selectedRowFromList)) {
            mPlayer.stop();
            mPlayer = MediaPlayer.create(this, songsIDs.get(songEquivalent.get(selectedRowFromList)));
            count = 0;
        }

        //play selected song
        if (count % 2 == 0) {
            mPlayer = MediaPlayer.create(this, songsIDs.get(songEquivalent.get(selectedRowFromList)));
            mPlayer.start();
            selectedSong = selectedRowFromList;
            l.getChildAt(position).setBackgroundColor(0xFF3F51B4);
            lastPosition = position;
        } else if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        count++;
    }

    private void ParseSongInfoFile() {

        //read info file
        InputStream inputStream = getListView().getContext().getResources().openRawResource(R.raw.songtitles);

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String[] songInfo;
        try {
            line = reader.readLine();
            while (line != null) {
                //parse info from eaxh line in the file
                songInfo = line.split("-");
                StringTokenizer st = new StringTokenizer(songInfo[1].trim(), "\",");
                String songName = st.nextToken().trim();
                String songArtist = st.nextToken().trim();
                songTitles.put(songInfo[0].trim(), songName + " - " + songArtist);
                songEquivalent.put(songName + " - " + songArtist, songInfo[0].trim());
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
