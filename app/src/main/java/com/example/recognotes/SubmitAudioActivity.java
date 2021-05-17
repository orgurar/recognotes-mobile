package com.example.recognotes;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

        // Ignore URI exposure for PDF download
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // init components
        recordingBPM = (TextView)findViewById(R.id.recording_bpm_text);
        recordingWavFile = (TextView)findViewById(R.id.recording_filename_text);
        recordSheetsNameInput = (EditText)findViewById(R.id.sheetsname_input_text);
        submitButton = (Button)findViewById(R.id.submit_request_button);
        backToMainButton = (ImageButton)findViewById(R.id.back_to_main_button);
        musicServiceMute = (Switch)findViewById(R.id.music_switch);

        // init intent parameters and read props from the main page
        Bundle intentParams = getIntent().getExtras();
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

                // assemble file's data
                String fileData = "{" +
                        "\"sample_rate\": " + prop_samplerate + ", " +
                        "\"bpm\": " + prop_bpm + ", " +
                        "\"sheets_title\": \"" + sheetsName + "\"" +
                        "}";

                // perform http post request
                UploadTask uploadTask = new UploadTask();
                uploadTask.execute(new String[]{prop_filename, fileData});
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
                    stopServiceMediaPlayer();
                } else {
                    // start the service again
                    startService(new Intent(getApplicationContext(), MyService.class));
                    ring = MediaPlayer.create(SubmitAudioActivity.this, R.raw.music);
                    ring.start();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopServiceMediaPlayer();
    }

    /** Media Player */
    /*
    stops the music
     */
    public void stopServiceMediaPlayer() {
        try {
            if (ring != null) {
                // stop the media from playing
                if (ring.isPlaying())
                    ring.stop();

                // get ready to the next time
                ring.release();
                ring = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class UploadTask extends AsyncTask<String, String, InputStream> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(InputStream inputStream) {
            super.onPostExecute(inputStream);
            if (inputStream != null) {
                String pdfFilePath = getExternalFilesDir("/") + "/record.pdf";
                File file = new File(pdfFilePath);

                // save PDF file
                try {
                    FileOutputStream f = new FileOutputStream(file);

                    byte[] buffer = new byte[2 * 1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1 ) {
                        f.write(buffer, 0, len);
                    }
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent target = new Intent(Intent.ACTION_VIEW);
                target.setDataAndType(Uri.fromFile(file), "application/pdf");
                target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                Intent intent = Intent.createChooser(target, "Open File");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SubmitAudioActivity.this, "File Upload Failed", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected InputStream doInBackground(String... strings) {
            final String backendAPI = "http://192.168.1.61:5000/proccess_audio";
            // create java file object
            File audioFile = new File(strings[0]);

            try {
                // assemble form data with the file and the file's data
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", audioFile.getName(), RequestBody.create(MediaType.parse("audio/wav"), audioFile))
                        .addFormDataPart("file_data", strings[1])
                        .build();

                // send post request to the backend server
                Request request = new Request.Builder()
                        .url(backendAPI)
                        .post(requestBody)
                        .build();

                // read the response as file blob
                OkHttpClient okHttpClient = new OkHttpClient();
                Response response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    return null;
                } else {
                    return response.body().byteStream();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}