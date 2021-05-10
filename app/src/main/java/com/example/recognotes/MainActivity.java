package com.example.recognotes;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // FFMpeg Instance
    FFmpeg fFmpeg;

    // Media Recorder Defaults
    private static final int MEDIA_RECORDER_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int MEDIA_RECORDER_OUTPUT_FORMAT = MediaRecorder.OutputFormat.THREE_GPP;
    private static final int MEDIA_RECORDER_AUDIO_ENCODER = MediaRecorder.AudioEncoder.AMR_NB;

    // Permission Defaults
    private static final String RECORD_PERMISSION = Manifest.permission.RECORD_AUDIO;
    private static final int PERMISSION_CODE = 21;

    // Media Recorder
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String recordFilePath;
    private String recordFilePathWAV;
    private int recordingBPM;

    // UI Components
    private ImageButton recordButton;
    private Chronometer recordTimer;
    private Switch recordMetronome;
    private EditText recordBPMInput;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // load FFMpeg library
        try {
            loadFFMpegLibrary();
        }
        catch(FFmpegNotSupportedException e) {
            e.printStackTrace();
        }

        // check if device already has permissions
        if (!checkPermissions())
            requestPermissions();

        // Init Views
        recordButton = (ImageButton)findViewById(R.id.record_button);
        recordTimer = (Chronometer)findViewById(R.id.record_timer);
        recordMetronome = (Switch)findViewById(R.id.main_metronome_switch);
        recordBPMInput = (EditText)findViewById(R.id.main_bpm_input);

        // set record button on click listener
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording) {
                    //Stop Recording
                    stopRecording();

                    // Change button image and set Recording state to false
                    recordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));
                    isRecording = false;

                    // using FFMpeg, convert the 3gp file to wav file
                    File threeGP = new File(recordFilePath);
                    try {
                        executeFFMpegCommand(new String[]{"-i " + threeGP.getAbsolutePath() + " -acodec pcm_u8 " + recordFilePathWAV});
                    }
                    catch (FFmpegCommandAlreadyRunningException e) {
                        e.printStackTrace();
                    }

                    // new intent to jump between screens
                    Intent intent = new Intent(MainActivity.this, SubmitAudioActivity.class);

                    // Create a new Bundle to send to the new Intent
                    Bundle intentParameters = new Bundle();
                    intentParameters.putInt("bpm", recordingBPM);
//                    intentParameters.putString("recording_path", recordFilePathWAV);
                    intentParameters.putString("recording_path", "wavfile path here");

                    // add bundle to intent
                    intent.putExtras(intentParameters);
                    startActivity(intent);
                    finish();
                } else {
                    // "Check permission to record audio
                    if(checkPermissions()) {
                        // validate BPM input
                        final String bpmText = recordBPMInput.getText().toString();
                        if (bpmText.equals("")) {
                            recordBPMInput.setError("BPM Field Cannot Be Empty While Recording");
                            return;
                        }
                        recordingBPM = Integer.parseInt(bpmText);
                        if (recordingBPM > 500 || recordingBPM < 0) {
                            recordBPMInput.setError("BPM value invalid! Should be between 0 - 500");
                            return;
                        }

                        // start metronome is needed
                        final boolean useMetronome = recordMetronome.isChecked();
                        if (useMetronome) {
                            // play metronome ticks
                            Toast.makeText(MainActivity.this, "Use Metronome", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            // do not play metronome ticks
                            Toast.makeText(MainActivity.this, "Do not use Metronome", Toast.LENGTH_SHORT).show();
                        }

                        // Start Recording
                        startRecording();

                        // Change button image and set Recording state to false
                        recordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_recording, null));
                        isRecording = true;
                    }
                    else {
                        requestPermissions();
                    }
                }
            }
        });
    }

    /** Recordings */

    /*
    Function will stop the recording timer and the audio recorder
    */
    private void stopRecording() {
        // Stop Timer
        recordTimer.stop();

        // Stop media recorder and set it to null for further use to record new audio
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    /*
    Will start the recording and generate a path for the file
     */
    private void startRecording() {
        // start timer from 0
        recordTimer.setBase(SystemClock.elapsedRealtime());
        recordTimer.start();

        // get app external directory path
        String recordPath = this.getExternalFilesDir("/").getAbsolutePath();

        // get current date and time
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss", Locale.US);
        Date now = new Date();
        String nowDate = formatter.format(now);

        // initialize filename variable with date and time at the end to ensure the new file wont overwrite previous file
        recordFilePath = recordPath + "/" + "Recording_" + nowDate + ".3gp";
        recordFilePathWAV = recordPath + "/" + "Recording_" + nowDate + ".wav";

        // setup Media Recorder for recording
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MEDIA_RECORDER_AUDIO_SOURCE);
        mediaRecorder.setOutputFormat(MEDIA_RECORDER_OUTPUT_FORMAT);
        mediaRecorder.setOutputFile(recordFilePath);
        mediaRecorder.setAudioEncoder(MEDIA_RECORDER_AUDIO_ENCODER);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Start Recording
        mediaRecorder.start();
    }

    /** Permissions */

    /*
    This function will check if the device has its needed permissions
     */
    private boolean checkPermissions() {
        //Check permission
        return ActivityCompat.checkSelfPermission(this, RECORD_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /*
    Function will request permissions from user's device
    */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                RECORD_PERMISSION,
        }, PERMISSION_CODE);
    }

    /*
    Will be called when user grants / denies permission
    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // check if result is the required permission's
        if(PERMISSION_CODE == requestCode) {
            // Permissions has granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                // Permissions denied
            else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    /** FFMpeg Library */

    public void loadFFMpegLibrary() throws FFmpegNotSupportedException {
        if (fFmpeg == null) {
            fFmpeg = FFmpeg.getInstance(this);

            fFmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Toast.makeText(MainActivity.this, "FFMpeg Library Failed to Load", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "FFMpeg Library Loaded Successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStart() {

                }

                @Override
                public void onFinish() {

                }
            });
        }
    }

    public void executeFFMpegCommand(String[] command) throws FFmpegCommandAlreadyRunningException {
        fFmpeg.execute(command, new ExecuteBinaryResponseHandler() {
            @Override
            public void onFailure(String message) {
                super.onFailure(message);
            }

            @Override
            public void onFinish() {
                super.onFinish();
            }

            @Override
            public void onProgress(String message) {
                super.onProgress(message);
            }

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onSuccess(String message) {
                super.onSuccess(message);
            }
        });
    }
}