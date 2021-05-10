package com.example.recognotes;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SubmitAudioActivity extends AppCompatActivity {
    // UI Components
    private TextView recordingBPM;
    private TextView recordingWavFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_audio);

        // init components
        recordingBPM = (TextView)findViewById(R.id.recording_bpm_text);
        recordingWavFile = (TextView)findViewById(R.id.recording_filename_text);

        // init intent parameters
        Bundle intentParams = getIntent().getExtras();

        final int prop_bpm = intentParams.getInt("bpm");
        final String prop_filename = intentParams.getString("recording_path");

        // set texts to the given props
        recordingBPM.setText("Recording's BPM: " + prop_bpm);
        recordingWavFile.setText("Audio File's Path: " + prop_filename);
    }
}