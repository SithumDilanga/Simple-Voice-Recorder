package com.simplevoice.android.voicerecorder;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 */
public class AudioListFragment extends Fragment implements AudioListAdapter.onItemListClick{

    private ConstraintLayout playerSheet;
    private BottomSheetBehavior bottomSheetBehavior; //to control bottom sheet behavior
    private RecyclerView audioList;
    private File[] allFiles;
    private AudioListAdapter audioListAdapter;
    private MediaPlayer mediaPlayer = null;
    private boolean isPlaying = false;
    private File fileToPlay; //to store files

    //--- Media player UI Elements ---
    private ImageButton playBtn;
    private TextView playerHeader;
    private TextView playerFileName;
    //--- End Media player UI Elements ---

    //--- seek bar elements ---
    private SeekBar playerSeekbar;
    private Handler seekbarHandler;
    private Runnable updateSeekbar;
    //--- End seek bar elements ---

    private AdView adView;


    public AudioListFragment() {
        // Required empty public constructor
    }

    //--- sorting allFiles list Pair class ---
    class Pair implements Comparable {
        public long t;
        public File f;

        public Pair(File file) {
            f = file;
            t = file.lastModified();
        }

        public int compareTo(Object o) {
            long u = ((Pair) o).t;
            return t > u ? -1 : t == u ? 0 : 1;
        }
    }

    //--- End sorting allFiles list Pair class ---

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_audio_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerSheet = view.findViewById(R.id.player_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(playerSheet);
        audioList = view.findViewById(R.id.audio_list_view);
        //Media player UI Elements
        playBtn = view.findViewById(R.id.player_play_btn);
        playerHeader = view.findViewById(R.id.player_header_title);
        playerFileName = view.findViewById(R.id.player_file_name);
        playerSeekbar = view.findViewById(R.id.player_seekbar);


        //--- FAN Ad integration ---

        // Initialize the Audience Network SDK
        AudienceNetworkAds.initialize(getActivity());

        adView = new AdView(getActivity(), "REAL_AD_ID_HERE", AdSize.BANNER_HEIGHT_50);

        // Find the Ad Container
        LinearLayout adContainer = (LinearLayout) view.findViewById(R.id.banner_container);

        // Add the ad view to your activity layout
        adContainer.addView(adView);

        // Request an ad
        adView.loadAd();

        //--- END FAN Ad integration ---

        //getting storage file path
        String path = getActivity().getExternalFilesDir("/").getAbsolutePath();
        File directory = new File(path);
        allFiles = directory.listFiles();


        //--- sorting allFiles list ---

        // Obtain the array of (file, timestamp) pairs.
        Pair[] pairs = new Pair[allFiles.length];
        for (int i = 0; i < allFiles.length; i++)
            pairs[i] = new Pair(allFiles[i]);

        // Sort them by timestamp at once.
        Arrays.sort(pairs);

        // Take the sorted pairs and extract only the file part, discarding the timestamp.
        for (int i = 0; i < allFiles.length; i++)
            allFiles[i] = pairs[i].f;

        //--- End sorting allFiles list ---

        //--- sorting allFiles list alternative method ---

        /*Arrays.sort(allFiles, new Comparator<File>(){
            public int compare(File f2, File f1){
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });*/

        //--- End sorting allFiles list alternative method ---

        //initializing Adapter and pass allFiles list to its constructor
        audioListAdapter = new AudioListAdapter(allFiles, this);
        audioList.setHasFixedSize(true);

        audioList.setLayoutManager(new LinearLayoutManager(getContext()));
        //audioListAdapter.setHasStableIds(true);
        audioList.setAdapter(audioListAdapter);

        //--- adjusting performance issues ---
        audioList.setItemViewCacheSize(20);
        audioList.setDrawingCacheEnabled(true);
        audioList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        audioListAdapter.notifyItemRemoved(audioList.getId());
        audioListAdapter.notifyItemChanged(audioList.getId());
        audioListAdapter.notifyItemInserted(audioList.getId());

        //--- End adjusting performance issues ---

        //audioListAdapter.notifyDataSetChanged();

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //when bottom state is hidden it will go into a collapsed state again
                //this will avoid media player from hiding when we tap it down
                if (newState == BottomSheetBehavior.STATE_HIDDEN){
                    //Sets the state of the bottom sheet. The bottom sheet will-
                    // transition to that state with animation.
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //we can't do anything here for this app
            }
        });

        //--- play button feature ---
        playBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                if (isPlaying){
                    pauseAudio();
                } else {
                    //otherwise it will crash when there is no audio file is loaded to media player at beginning
                    if (fileToPlay != null) {
                        resumeAudio();
                    } else {
                        Toast.makeText(getContext(), "Please select a voice record to play",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        //--- End play button feature ---

        playerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                pauseAudio();
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (fileToPlay != null) {
                    int progress = seekBar.getProgress();//getting current progress
                    mediaPlayer.seekTo(progress); //media player start playing from the current progress
                    resumeAudio();
                }
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClickListener(File file, int position) {
        fileToPlay = file;
        if (isPlaying){
            //stop playing the audio
            stopAudio();
            playAudio(fileToPlay);

        } else {
            //if there is an audio file to play it will play the audio.if not it will say some error
            if (fileToPlay != null){
                //play the audio
                playAudio(fileToPlay);
            } else {

                //TODO: have to find a way to get here

            }

        }
    }

    //pause the audio when user tap play button while audio is playing
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void pauseAudio(){
        if (fileToPlay != null) {
            mediaPlayer.pause();
            //change play button into pause button
            playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn, null));
            isPlaying = false;
            seekbarHandler.removeCallbacks(updateSeekbar); //handler needs to be stopped when paused the audio
        }
    }

    //resume the audio when user tap play button when audio is paused
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void resumeAudio(){
        mediaPlayer.start();
        //change play button into pause button
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn,null));
        isPlaying = true;
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAudio() {
            //set the player button to pause
            playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_play_btn, null));
            playerHeader.setText("Stopped");
            isPlaying = false;
            mediaPlayer.stop();// it will play overlap if it didn't stop
            seekbarHandler.removeCallbacks(updateSeekbar); //handler needs to be stopped when stop the audio
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void playAudio(final File fileToPlay) {

        mediaPlayer = new MediaPlayer();
        //start playing audio the we assigned to the medaiplayer

        //media player state changed into the expanded when audio is playing
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        try {
            //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //Sets the data source (file-path or http/rtsp URL) to use.
            mediaPlayer.setDataSource(fileToPlay.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //set the player button to pause
        playBtn.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.player_pause_btn, null));
        playerFileName.setText(fileToPlay.getName());
        playerHeader.setText("Playing");
        isPlaying = true;

        //when audio completes playing
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCompletion(MediaPlayer mp) {
                    stopAudio();
                    playerHeader.setText("finished");

                    //--- be able to play the already loaded recording again ---
                    mediaPlayer.reset();
                    try {
                        mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //--- End be able to play the already loaded recording again ---
            }
        });

        //getting audio file time duration and set it to the seekbar as maximum progress
        playerSeekbar.setMax(mediaPlayer.getDuration());

        seekbarHandler = new Handler();
        updateRunnable();
        seekbarHandler.postDelayed(updateSeekbar, 0);
    }

    private void updateRunnable() {
        updateSeekbar = new Runnable() {
            @Override
            public void run() {
                //get current playback position and sets the current progress to the specified value
                playerSeekbar.setProgress(mediaPlayer.getCurrentPosition());

                //Causes the Runnable r to be added to the message queue,
                // to be run after the specified amount of time elapses.
                seekbarHandler.postDelayed(this, 0);//can change the seekbar realtime progress delay
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onStop() {
        super.onStop();
        if (isPlaying) {
            stopAudio(); //when close the app it should stop playing
        }
    }
}
