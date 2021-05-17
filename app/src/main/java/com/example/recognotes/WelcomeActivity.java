package com.example.recognotes;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {
    // Media Player for music service
    private MediaPlayer mediaPlayer;

    // Firebase
    private FirebaseAuth fAuth;

    // UI parts
    private Switch musicServiceMute;
    private Button userRegister;
    private Button emailSignIn;
    private Button guestSignIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // init UI
        musicServiceMute = (Switch)findViewById(R.id.music_switch);
        userRegister = (Button)findViewById(R.id.user_register);
        emailSignIn = (Button)findViewById(R.id.user_sign_in);
        guestSignIn = (Button)findViewById(R.id.guset_sign_in);

        // get firebase instance
        fAuth = FirebaseAuth.getInstance();

        // start the music service
        startService(new Intent(getApplicationContext(), MyService.class));
        mediaPlayer = MediaPlayer.create(WelcomeActivity.this, R.raw.music);
        mediaPlayer.start();

        // sign in
        emailSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // redirect user: Welcome -> Login
                stopServiceMediaPlayer();
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // signup
        userRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // redirect user: Welcome -> Signup
                stopServiceMediaPlayer();
                Intent intent = new Intent(WelcomeActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        // guest (no sign in needed)
        guestSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // redirect user: Welcome -> Main
                stopServiceMediaPlayer();
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
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
                    mediaPlayer = MediaPlayer.create(WelcomeActivity.this, R.raw.music);
                    mediaPlayer.start();
                }
            }
        });
    }

    /*
    When the user presses the back button it goes to the Main Activity as the logged user (if any)
     */
    @Override
    public void onBackPressed() {
        stopServiceMediaPlayer();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI();
            stopServiceMediaPlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopServiceMediaPlayer();
    }

    /*
    When the login succeed, it passes the screen to the main activity
     */
    private void updateUI() {
        Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /*
    stops the music
     */
    public void stopServiceMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                // stop the media from playing
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();

                // get ready to the next time
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}