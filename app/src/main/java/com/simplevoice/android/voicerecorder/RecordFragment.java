package com.simplevoice.android.voicerecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.facebook.ads.AdSize;
import com.facebook.ads.*;
import com.facebook.ads.AudienceNetworkAds;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//import com.facebook.ads.AdView;

//import com.facebook.ads.AdView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements View.OnClickListener {

    private NavController navController;
    private ImageButton listBtn;
    private ImageButton recordBtn;
    private boolean isRecording = false;
    private String recordPermission = Manifest.permission.RECORD_AUDIO;
    private String storageWritePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private String storageReadPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
    private final static int PERMISSION_CODE = 23;
    private MediaRecorder mediaRecorder;
    private String recordFile;
    private Chronometer timer;
    private TextView fileNameText;


    //private AdView adView1;
    private AdView adView;

    //public static final int SAMPLING_RATE = 44100;


    public RecordFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_record, container, false);



    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view); //initializing navController
        listBtn = view.findViewById(R.id.record_list_btn);
        recordBtn = view.findViewById(R.id.record_btn);
        timer = view.findViewById(R.id.record_timer);
        fileNameText = view.findViewById(R.id.record_filename);

        listBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);

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

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.record_list_btn:

                if(isRecording){ //if its still in recording

                    //showing an alert dialog

                    // get common_alert_dialog.xml view
                    final LayoutInflater layoutInflater = LayoutInflater.from(v.getContext());

                    final View promptView = layoutInflater.inflate(R.layout.common_alert_dialog, null);

                    final AlertDialog alertDialogBuilder = new AlertDialog.Builder(v.getContext()).create();

                    // set common_alert_dialog.xml to alertdialog builder
                    alertDialogBuilder.setView(promptView);

                    Button alertBtnPositive = promptView.findViewById(R.id.commonAlertPosBtn);
                    Button alertBtnNegative = promptView.findViewById(R.id.commonAlertNegBtn);

                    //doesn't cancel if tap alert dialog excluded area
                    alertDialogBuilder.setCancelable(false);

                    alertBtnPositive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isRecording = false;
                            stopRecording();
                            //navigate from RecordFragment to AudioListFragment
                            navController.navigate(R.id.action_recordFragment_to_audioListFragments);

                            alertDialogBuilder.dismiss();
                        }
                    });

                    alertBtnNegative.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            alertDialogBuilder.cancel();
                        }
                    });

                    alertDialogBuilder.setTitle("Audio still recording");
                    alertDialogBuilder.setMessage("Are you sure, you want to stop the recording");
                    alertDialogBuilder.show();


                } else {
                    //navigate from RecordFragment to AudioListFragment
                    navController.navigate(R.id.action_recordFragment_to_audioListFragments);
                }
                break;

            case R.id.record_btn:
                if (isRecording) {
                    //stop recording
                    stopRecording();

                    //setting record button into a different image button
                    recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));
                    isRecording = false;
                } else {
                    //checking permissions before start recording
                    if (checkPermissions()) {
                        //start recording
                        startRecording();
                        recordBtn.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_recording, null));
                        isRecording = true;
                    }
                }
                break;

        }
    }


    private void stopRecording() {
        timer.stop(); //stopping timer
        fileNameText.setText("File saved : " + recordFile); //displaying record file name text after stopped recording
        mediaRecorder.stop(); //stop recording audio
        mediaRecorder.release(); //Releases audio resource associated with this MediaRecorder object
        mediaRecorder = null;

    }

    private void startRecording() {
        timer.setBase(SystemClock.elapsedRealtime()); //starting from beginning when stop and start again
        timer.start(); //starting timer

        //getting file directory in the storage
        String recordPath = getActivity().getExternalFilesDir("/").getAbsolutePath();

        //create a new date format
        SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_hh_mm_ss", Locale.CANADA);

        //getting current date
        Date now = new Date();

        //setting saving file name
        recordFile = "Rec " + formatter.format(now) + ".mp3";

        //displaying saving file name of current recording
        fileNameText.setText("File name : " + recordFile);

        mediaRecorder = new MediaRecorder(); //initializing MediaRecorder
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //setting audio source as phone mic
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //setting output format as MPEG_4
        mediaRecorder.setOutputFile(recordPath + "/" + recordFile); //setting saving file path
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //setting mp3 quality sampling rate.but this create annoying noice in the recording
        //mediaRecorder.setAudioSamplingRate(SAMPLING_RATE);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaRecorder.start(); //start recording
    }

    //--- checking permission ---
    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(getContext(), recordPermission) +
                ActivityCompat.checkSelfPermission(getContext(), storageReadPermission) +
                ActivityCompat.checkSelfPermission(getContext(),storageWritePermission) == PackageManager.PERMISSION_GRANTED){

            return true;
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[] {recordPermission,
                    storageReadPermission, storageWritePermission}, PERMISSION_CODE);
            return false;
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){  //Callback for the result from requesting permissions. This method is invoked for every call on

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSION_CODE: {
                if (grantResults.length > 0 ) {
                    boolean storageWritePermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean storageReadPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean recordPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageWritePermission && storageReadPermission && recordPermission){
                        Toast.makeText(getActivity(), "Storage Permission Granted", Toast.LENGTH_SHORT).show();
                    }else {

                        Toast.makeText(getActivity(), "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                    }

                }
                break;
            }

        }
    }

    //--- End checking permission ---


    @Override
    public void onStop() {
        super.onStop();
        if (isRecording) {
            stopRecording(); //then audio file won't crash if this fragment is closed
        }
    }
}
