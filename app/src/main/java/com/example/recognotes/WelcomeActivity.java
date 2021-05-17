package com.example.recognotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class WelcomeActivity extends AppCompatActivity {
    // Media Player for music service
    private MediaPlayer ring;

    // Firebase
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth fAuth;
    private static final int RC_SIGN_IN = 9001;

    // UI parts
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch musicServiceMute;
    private Button emailSignIn;
    private Button guestSignIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // init UI
        musicServiceMute = (Switch)findViewById(R.id.music_switch);
        emailSignIn = (Button)findViewById(R.id.user_sign_in);
        guestSignIn = (Button)findViewById(R.id.guset_sign_in);

        // get firebase instance
        fAuth = FirebaseAuth.getInstance();

        // start the music service
        startService(new Intent(getApplicationContext(), MyService.class));
        ring = MediaPlayer.create(WelcomeActivity.this, R.raw.music);
        ring.start();

        // login listeners
        emailSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServiceMediaPlayer();
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        guestSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    ring = MediaPlayer.create(WelcomeActivity.this, R.raw.music);
                    ring.start();
                }
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
            }
        }
    }

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
            SubmitLogin();
            stopServiceMediaPlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopServiceMediaPlayer();
    }

    /** Firebase Login */

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        fAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            SubmitLogin();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(WelcomeActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void SubmitLogin() {
        Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
}