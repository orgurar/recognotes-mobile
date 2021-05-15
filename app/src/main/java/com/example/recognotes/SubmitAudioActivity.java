package com.example.recognotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

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
                String fileData = "{" +
                        "\"sample_rate\": " + prop_samplerate + ", " +
                        "\"bpm\": " + prop_bpm + ", " +
                        "\"sheets_title\": " + sheetsName +
                        "}";

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
                    startService(new Intent(getApplicationContext(), MyService.class));
                    ring = MediaPlayer.create(SubmitAudioActivity.this, R.raw.music);
                    ring.start();
                }
            }
        });
    }

//    private void makeAPICall(String filePath, int bpm, int sampleRate, String sheetsName) throws IOException {
//        String backendAPI = "http://192.168.1.26:5000/proccess_audio";
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        HttpPost uploadFile = new HttpPost(backendAPI);
//        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//
//
//
//        builder.addTextBody("file_data", fileData, ContentType.TEXT_PLAIN);
//
//// This attaches the file to the POST:
//        File f = new File(filePath);
//        builder.addBinaryBody(
//                "file",
//                new FileInputStream(f),
//                ContentType.APPLICATION_OCTET_STREAM,
//                f.getName()
//        );
//
//        HttpEntity multipart = builder.build();
//        uploadFile.setEntity(multipart);
//        CloseableHttpResponse response = httpClient.execute(uploadFile);
//        HttpEntity responseEntity = response.getEntity();
//        Toast.makeText(this, responseEntity.getContent().toString(), Toast.LENGTH_SHORT).show();
//    }

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
        stopServiceMediaPlayer();
    }

    /** Media Player */
    /*
    stops the music
     */
    public void stopServiceMediaPlayer() {
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

    public class UploadTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s!=null){
                Toast.makeText(SubmitAudioActivity.this, "File Uploaded", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(SubmitAudioActivity.this, "File Upload Failed", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            File audioFile = new File(strings[0]);
            final String backendAPI = "http://127.0.0.1:5000/proccess_audio";

            try {
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", audioFile.getName(), RequestBody.create(MediaType.parse("*/*"), audioFile))
                        .addFormDataPart("file_data", strings[0])
                        .build();
                Request request = new Request.Builder()
                        .url(backendAPI)
                        .post(requestBody)
                        .build();

                OkHttpClient okHttpClient = new OkHttpClient();
                //now progressbar not showing properly let's fixed it
                Response response = okHttpClient.newCall(request).execute();
                if (response != null && response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}