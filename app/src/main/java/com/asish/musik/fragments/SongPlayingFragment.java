package com.asish.musik.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asish.musik.models.CurrentSongHelper;
import com.asish.musik.databases.MusikDatabase;
import com.asish.musik.R;
import com.asish.musik.models.Songs;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SongPlayingFragment extends Fragment implements SensorEventListener {

    int songPosition;
    Thread thread;
    TextView songArtistView;
    TextView songTitleView;
    Button playPauseImageButton;
    Button previousImageButton;
    Button nextImageButton;
    ImageView songImg;
    TextView rightTime, leftTime;
    int currentPosition = 0;
    ImageButton fab;

    SeekBar seekBar;
    static ArrayList<Songs> mySongs;

    static MediaPlayer mediaPlayer;
    MusikDatabase favoriteContent;


    static CurrentSongHelper currentSongHelper = new CurrentSongHelper();


    // variables for shake detection
    private static final float SHAKE_THRESHOLD = 9f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;
    private SensorManager mSensorMgr;

    //After launch
    static String MY_PREFS_NAME = "ShakeFeature";

    Activity myActivity;

    public SongPlayingFragment() {
        // Required empty public constructor
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song_playing, container, false);
        getActivity().setTitle("Now Playing");

        currentSongHelper.isPlaying = true;
//        currentSongHelper.isLoop = false;
        currentSongHelper.isShuffle = false;

        // Get Views..
        songTitleView = view.findViewById(R.id.songTextLabel);
        songArtistView = view.findViewById(R.id.songArtistLabel);
        playPauseImageButton = view.findViewById(R.id.btnPause);
        previousImageButton = view.findViewById(R.id.btnPrevious);
        nextImageButton = view.findViewById(R.id.btnNext);
        seekBar = view.findViewById(R.id.seekBar);
        songImg = view.findViewById(R.id.songImg);
        songTitleView.setSelected(true);
        leftTime = view.findViewById(R.id.leftTime);
        rightTime = view.findViewById(R.id.rightTime);
        fab = view.findViewById(R.id.button_favorite);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Get Data from MainActivity..
//        Intent intent = myActivity.getIntent();
        favoriteContent = new MusikDatabase(myActivity);
        String songArtist = null;
        String path = null;
        String songTitle = null;
        long SongId;
       try {
           songArtist = getArguments().getString("songArtist");
           path = getArguments().getString("path");
           songTitle = getArguments().getString("songTitle");
           SongId = getArguments().getInt("SongId", 0);
           songPosition = getArguments().getInt("songPosition", 0);
           mySongs = (ArrayList) getArguments().getParcelableArrayList("songData");
           getAlbumArt(mySongs.get(songPosition).getSongData());

           currentSongHelper.songPath = path;
           currentSongHelper.songTitle = songTitle;
           currentSongHelper.songArtist = songArtist;
           currentSongHelper.songId = SongId;
           currentSongHelper.currentPosition = songPosition;
           

       } catch (Exception e) {
           e.printStackTrace();
       }

        String fromFavBottomBar = (String) getArguments().get("FavBottomBar");
        if(fromFavBottomBar != null) {
            mediaPlayer = FavoriteFragment.mediaPlayer;
        }
        else {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            try {
                Uri u = Uri.parse(path);
                mediaPlayer = MediaPlayer.create(myActivity, u);
                mediaPlayer.start();
                updateThread();
                seekBar.setMax(mediaPlayer.getDuration());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Seek bar setup

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    mediaPlayer.seekTo(progress);
                }
                int currentPos = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                int diff = duration - currentPos;
                leftTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(currentPos), TimeUnit.MILLISECONDS.toSeconds(currentPos)%60));
                rightTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(diff), TimeUnit.MILLISECONDS.toSeconds(diff)%60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        // Set Data..
        songTitleView.setText(songTitle);
        songArtistView.setText(songArtist);
        clickHandler();


    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSensorMgr = (SensorManager) myActivity.getSystemService(Context.SENSOR_SERVICE);

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mediaPlayer.isPlaying()) {
            currentSongHelper.isPlaying = true;
            playPauseImageButton.setBackgroundResource(R.drawable.icon_pause);
        } else {
            currentSongHelper.isPlaying = false;
            playPauseImageButton.setBackgroundResource(R.drawable.icon_play);
        }

        mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();

        mSensorMgr.unregisterListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myActivity = (Activity) context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        myActivity = activity;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(Math.pow(x, 2) +
                        Math.pow(y, 2) +
                        Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;
//                Toast.makeText(this, "Acceleration is " + acceleration + "m/s^2", Toast.LENGTH_SHORT).show();

                if (acceleration > SHAKE_THRESHOLD) {
                    mLastShakeTime = curTime;
                    SharedPreferences prefs = myActivity.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE);
                    boolean isAllowed = prefs.getBoolean("feature", false);
                    if (isAllowed) {
                        nextMusic();
//                        Toast.makeText(myActivity, "Shaked..", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Load Album Art..
    public void getAlbumArt(String songDta) {
        MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
        try {
            metaRetriver.setDataSource(songDta);
//            Toast.makeText(myActivity, "Can't Play..", Toast.LENGTH_SHORT).show();
            byte[] art = metaRetriver.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory
                    .decodeByteArray(art, 0, art.length);
            songImg.setImageBitmap(songImage);
        } catch (Exception e) {
            songImg.setImageResource(R.drawable.cover_img);
        }
    }

    public void updateThread() {
        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(mediaPlayer != null) {
                        thread.sleep(50);
                        myActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int newPosition = mediaPlayer.getCurrentPosition();
                                int newDuration = mediaPlayer.getDuration();
                                seekBar.setMax(newDuration);
                                seekBar.setProgress(newPosition);
                                leftTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getCurrentPosition()), TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getCurrentPosition())%60));
                                rightTime.setText(String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()), TimeUnit.MILLISECONDS.toSeconds(mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition())%60));
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    // Click Handler
    public void clickHandler() {

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (favoriteContent.checkifIdExists((int)currentSongHelper.songId) ) {
                    fab.setBackgroundResource(R.drawable.button_favorite);
                    favoriteContent.deleteFavourite((int)currentSongHelper.songId);
                    /*Toast is prompt message at the bottom of screen indicating that an
                    action has been performed*/
                    Toast.makeText(myActivity, "Removed from Favorites",
                            Toast.LENGTH_SHORT).show();
                } else {
                    /*If the song was not a favorite, we then add it to the favorites using
                    the method we made in our database*/
                    fab.setBackgroundResource(R.drawable.fav_on);
                    favoriteContent.storeasFavourite((int)currentSongHelper.songId,
                            currentSongHelper.songArtist, currentSongHelper.songTitle, currentSongHelper.songPath);
                    Toast.makeText(myActivity, "Added to Favorites",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        playPauseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying()) {
                    pauseMusic();
                }
                else
                    startMusic();
            }
        });

        nextImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextMusic();
            }
        });

        previousImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousMusic();
            }
        });
    }

    public void pauseMusic() {
        if(mediaPlayer != null) {
            mediaPlayer.pause();
            playPauseImageButton.setBackgroundResource(R.drawable.icon_play);
        }
    }

    public void startMusic() {
        if(mediaPlayer != null) {
            mediaPlayer.start();
            updateThread();
            playPauseImageButton.setBackgroundResource(R.drawable.icon_pause);
        }
    }

    public void nextMusic() {
        mediaPlayer.stop();
        mediaPlayer.release();
        songPosition = ((songPosition+1)%mySongs.size());
//                nowPlaying++;
//                position = ((position+1)%duration);
        Uri u = Uri.parse(mySongs.get(songPosition).getSongData());
        mediaPlayer = MediaPlayer.create(myActivity, u);
//                sName = mySongs.get(position).getName();
        songTitleView.setText(mySongs.get(songPosition).getSongTitle());
        songArtistView.setText(mySongs.get(songPosition).getArtist());
        try{
            getAlbumArt(mySongs.get(songPosition).getSongData());
//            Toast.makeText(myActivity, "Can't Play..", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        startMusic();
    }

    public void previousMusic() {
        mediaPlayer.stop();
        mediaPlayer.release();
        songPosition = ((songPosition-1<0)?(mySongs.size()-1):(songPosition-1));
        Uri u = Uri.parse(mySongs.get(songPosition).getSongData());
        mediaPlayer = MediaPlayer.create(myActivity, u);
//                sName = mySongs.get(position).getName();
        try{
            getAlbumArt(mySongs.get(songPosition).getSongData());
//            Toast.makeText(myActivity, "Can't Play..", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        songTitleView.setText(mySongs.get(songPosition).getSongTitle());
        songArtistView.setText(mySongs.get(songPosition).getArtist());
        startMusic();
    }

    public void updateTextViews(String songTitle, String songArtist) {
        String songTitleUpdated = songTitle;
        String songArtistUpdated=songArtist;

        if (songTitle.equalsIgnoreCase("<unknown>")){
            songTitleUpdated= "unknown";
        }
        if (songArtist.equalsIgnoreCase("<unknown>")){
            songArtistUpdated= "unknown";
        }
        songTitleView.setText(songTitleUpdated);
        songArtistView.setText(songArtistUpdated);
    }

    public void processInformation(MediaPlayer mediaPlayer) {

    }
}
