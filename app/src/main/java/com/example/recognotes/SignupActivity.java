package com.example.recognotes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupActivity extends AppCompatActivity {
    // UI fields
    private EditText fullName;
    private EditText email;
    private EditText password;
    private Button signupButton;
    private TextView gotoLoginText;

    // FireBase Instance
    private FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // pick UI components from screen
        fullName = (EditText)findViewById(R.id.signup_input_fullname);
        email = (EditText)findViewById(R.id.signup_input_email);
        password = (EditText)findViewById(R.id.signup_input_password);
        signupButton = (Button)findViewById(R.id.signup_finish_button);
        gotoLoginText = (TextView)findViewById(R.id.signup_login);

        // Firebase initiate
        fAuth = FirebaseAuth.getInstance();

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String fullNameString = fullName.getText().toString();
                final String emailString = email.getText().toString().trim();
                final String passwordString = password.getText().toString().trim();
                boolean fieldsError = false;

                // fields validation
                if (TextUtils.isEmpty(fullNameString)) {
                    fullName.setError("Name is Required.");
                    fieldsError = true;
                }
                if(TextUtils.isEmpty(emailString)) {
                    email.setError("Email is Required.");
                    fieldsError = true;
                }
                if(TextUtils.isEmpty(passwordString)) {
                    password.setError("Password is Required.");
                    fieldsError = true;
                }
                if(passwordString.length() < 6) {
                    password.setError("Password Must be at least 6 Characters");
                    fieldsError = true;
                }

                // the app should not continue if there was an error
                if (fieldsError)
                    return;

                // register the user in firebase's storage
                fAuth.createUserWithEmailAndPassword(emailString, passwordString).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            // get the connected user and update its name
                            FirebaseUser user = fAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullNameString).build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful())
                                        // everything is right, user created
                                        Toast.makeText(SignupActivity.this, "User Created.", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                    }
                                });
                        } else {
                            // print the error
                            Toast.makeText(SignupActivity.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // login button
        gotoLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // redirect user: Signup -> Login
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

    }
}