package com.example.recognotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;

public class SubmitAudioActivity extends AppCompatActivity {
    // Media Player for music service
    private MediaPlayer ring;

    // UI Components
    private TextView recordingBPM;
    private TextView recordingWavFile;
    private EditText recordSheetsNameInput;
    private Button submitButton;
    private ImageButton backToMainButton;
    private Switch musicServiceMute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_audio);

        // init components
        recordingBPM = (TextView)findViewById(R.id.recording_bpm_text);
        recordingWavFile = (TextView)findViewById(R.id.recording_filename_text);
        recordSheetsNameInput = (EditText)findViewById(R.id.sheetsname_input_text);
        submitButton = (Button)findViewById(R.id.submit_request_button);
        backToMainButton = (ImageButton)findViewById(R.id.back_to_main_button);
        musicServiceMute = (Switch)findViewById(R.id.music_switch);

        // init intent parameters
        Bundle intentParams = getIntent().getExtras();

        // read props from the main page
        final int prop_bpm = intentParams.getInt("bpm");
        final int prop_samplerate = intentParams.getInt("sample_rate");
        final String prop_filename = intentParams.getString("recording_path");

        // set texts to the given props
        recordingBPM.setText("Recording's BPM: " + prop_bpm);
        recordingWavFile.setText("Recording's Sample Rate: " + prop_samplerate);

        // start the music service
        startService(new Intent(getApplicationContext(), MyService.class));
        ring = MediaPlayer.create(SubmitAudioActivity.this, R.raw.music);
        ring.start();

        // submit button onclick
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // firstly, validate input
                final String sheetsName = recordSheetsNameInput.getText().toString();
                if (sheetsName.equals("")) {
                    recordSheetsNameInput.setError("Sheets Name Must Not Be Empty");
                    return;
                }

                // now send the RESTful API request to the server
                File file = new File(prop_filename.substring(0, prop_filename.length() - 4) + ".3gp");
                playAudio(file);
            }
        });

        // back to main onClick
        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SubmitAudioActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // set listener to service's switch
        musicServiceMute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    stopMediaPlayer();
                } else {
                    startService(new Intent(getApplicationContext(), MyService.class));
                    ring = MediaPlayer.create(SubmitAudioActivity.this, R.raw.music);
                    ring.start();
                }
            }
        });
    }

    //TMP
    private void playAudio(File fileToPlay) {

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(fileToPlay.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Play the audio
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.stop();
            }
        });
    }
    //END TMP

    @Override
    protected void onStop() {
        super.onStop();
        stopMediaPlayer();
    }

    /** Media Player */
    /*
    stops the music
     */
    public void stopMediaPlayer() {
        try {
            if (ring != null) {
                if (ring.isPlaying())
                    ring.stop();
                ring.release();
                ring = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}