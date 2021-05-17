package com.example.recognotes;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // Audio Wav Recorder
    private WavAudioRecorder wavAudioRecorder;

    // Permission Defaults
    private static final String RECORD_PERMISSION = Manifest.permission.RECORD_AUDIO;
    private static final String STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String INTERNET_PERMISSION = Manifest.permission.INTERNET;
    private static final int PERMISSION_CODE = 21;

    // Metronome and Media Player
    private MediaPlayer metronomePlayer;
    private final Timer metronomeTimer = new Timer("Metronome Timer", true);
    private final TimerTask metronomeTone = new TimerTask() {
        @Override
        public void run() {
            metronomePlayer = MediaPlayer.create(MainActivity.this, R.raw.beep);
            metronomePlayer.start();
        }
    };

    // Broadcast Receiver
    private final BroadcastReceivers myReceiver = new BroadcastReceivers();
    // This function will set the broadcast receiver to listen
    private void setBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        // Register the receiver using the activity context.
        this.registerReceiver(myReceiver, filter);
    }

    // recording's properties
    private int recordingBPM;
    private String wavFilePath;

    // UI Components
    private ImageButton recordButton;
    private Chronometer recordTimer;
    private Switch recordMetronome;
    private EditText recordBPMInput;
    private ImageButton signOutButton;

    // Firebase
    FirebaseAuth fAuth;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // activate the broadcast receiver
        setBroadcastReceiver();

        // check if device already has permissions
        if (!checkPermissions())
            requestPermissions();

        // initiate firebase instance
        fAuth = FirebaseAuth.getInstance();

        String connectedUsersName;

        // Say hello to the connected user
        if (fAuth.getCurrentUser() != null)
            connectedUsersName = fAuth.getCurrentUser().getDisplayName();
        else
            connectedUsersName = "Guest";
        Toast.makeText(this, "Hello " + connectedUsersName, Toast.LENGTH_SHORT).show();

        // initialize wav recorder
        wavFilePath = this.getExternalFilesDir("/").getAbsolutePath() + "/record.wav";
        wavAudioRecorder = WavAudioRecorder.getInstanse();
        wavAudioRecorder.setOutputFile(wavFilePath);

        // Init Views
        recordButton = (ImageButton)findViewById(R.id.record_button);
        recordTimer = (Chronometer)findViewById(R.id.record_timer);
        recordMetronome = (Switch)findViewById(R.id.main_metronome_switch);
        recordBPMInput = (EditText)findViewById(R.id.main_bpm_input);
        signOutButton = (ImageButton)findViewById(R.id.sign_out_button);

        // set record button on click listener
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // should start recording
                if (WavAudioRecorder.State.INITIALIZING == wavAudioRecorder.getState()) {
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

                        //Start timer from 0
                        recordTimer.setBase(SystemClock.elapsedRealtime());
                        recordTimer.start();

                        // start metronome is needed
                        final boolean useMetronome = recordMetronome.isChecked();
                        if (useMetronome) {
                            // calculate BPM and play metronome ticks
                            int oneBeep = (60000 / recordingBPM); // in ms
                            metronomeTimer.scheduleAtFixedRate(metronomeTone, 0, oneBeep);
                        }

                        // Start Recording
                        wavAudioRecorder.prepare();
                        wavAudioRecorder.start();

                        // Change button image and set Recording state to false
                        recordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_recording, null));
                    }
                    else {
                        requestPermissions();
                    }
                }
                else if (WavAudioRecorder.State.ERROR == wavAudioRecorder.getState()) {
                    // error
                    recordTimer.stop();
                    wavAudioRecorder.release();
                    wavAudioRecorder = WavAudioRecorder.getInstanse();
                    wavAudioRecorder.setOutputFile(wavFilePath);

                    // Change button image and set Recording state to false
                    recordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));
                }
                else {
                    // Stop timer
                    recordTimer.stop();

                    //Stop Recording
                    wavAudioRecorder.stop();
                    wavAudioRecorder.reset();

                    // Change button image and set Recording state to false
                    recordButton.setImageDrawable(getResources().getDrawable(R.drawable.record_btn_stopped, null));

                    // new intent to jump between screens
                    Intent intent = new Intent(MainActivity.this, SubmitAudioActivity.class);

                    // Create a new Bundle to send to the new Intent
                    Bundle intentParameters = new Bundle();
                    intentParameters.putInt("bpm", recordingBPM);
                    intentParameters.putInt("sample_rate", 44100);
                    intentParameters.putString("recording_path", wavFilePath);

                    // add bundle to intent
                    intent.putExtras(intentParameters);
                    startActivity(intent);
                    finish();
                }
            }
        });

        // open a dialog and ask weather to sign out or not
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Sign out")
                        .setMessage("Would you like to disconnect from this user?")
                        .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // disconnect from firebase user
                                if(fAuth.getCurrentUser() != null) {
                                    Toast.makeText(MainActivity.this, "You signed out " + fAuth.getCurrentUser().getDisplayName(), Toast.LENGTH_LONG).show();
                                    fAuth.signOut();
                                    // redirect user: Main -> Welcome
                                    Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                                    startActivity(intent);
                                }
                                else {
                                    Toast.makeText(MainActivity.this, "There is no users signed in", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("no", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
                        })
                        .create().show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // cancel beep if active
        metronomeTimer.cancel();
    }

    /** Permissions */

    /*
    This function will check if the device has its needed permissions
     */
    private boolean checkPermissions() {
        //Check permission
        return ActivityCompat.checkSelfPermission(this, RECORD_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, INTERNET_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /*
    Function will request permissions from user's device
    */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                RECORD_PERMISSION,
                STORAGE_PERMISSION,
                INTERNET_PERMISSION,
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
}