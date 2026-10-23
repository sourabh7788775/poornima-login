package com.example.poornimalogin;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNewUsername, etEmail, etNewPassword, etConfirmPassword;
    private Button btnCreateAccount, btnBackToLogin;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference profilesRef, usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNewUsername = findViewById(R.id.etNewUsername);
        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database;
        try {
            database = FirebaseDatabase.getInstance();
        } catch (Exception e) {
            database = FirebaseDatabase.getInstance("https://poornimalogin-default-rtdb.firebaseio.com/");
        }

        profilesRef = database.getReference("profiles");
        usersRef = database.getReference("users");

        btnCreateAccount.setOnClickListener(v -> registerUser());
        btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String username = etNewUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (username.length() < 3) {
            etNewUsername.setError("Username must contain at least 3 characters");
            etNewUsername.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etNewPassword.setError("Use at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        setRegistrationBusy(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Exception error = task.getException();
                        String errMsg = safeError(error);

                        // If Firebase Auth Email/Password is disabled in Firebase Console (CONFIGURATION_NOT_FOUND)
                        // or unavailable, fall back to registering in Realtime Database.
                        if (errMsg.contains("CONFIGURATION_NOT_FOUND") || errMsg.contains("internal error")) {
                            registerInDatabase(username, email, password);
                            return;
                        }

                        setRegistrationBusy(false);
                        Toast.makeText(this, "Registration failed: " + errMsg,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        setRegistrationBusy(false);
                        Toast.makeText(this, "Account created but user could not be loaded",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("username", username);
                    profile.put("email", email);
                    profile.put("createdAt", ServerValue.TIMESTAMP);

                    profilesRef.child(user.getUid()).setValue(profile)
                            .addOnCompleteListener(this, profileTask -> {
                                setRegistrationBusy(false);
                                firebaseAuth.signOut();

                                if (profileTask.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Registered successfully. Log in using your email.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this,
                                            "Account created, but profile save failed: "
                                                    + safeError(profileTask.getException())
                                                    + ". You can still log in using your email.",
                                            Toast.LENGTH_LONG).show();
                                }
                                finish();
                            });
                });
    }

    private void registerInDatabase(String username, String email, String password) {
        String key = usersRef.push().getKey();
        if (key == null) {
            key = String.valueOf(System.currentTimeMillis());
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("password", password);
        userData.put("createdAt", ServerValue.TIMESTAMP);

        usersRef.child(key).setValue(userData)
                .addOnCompleteListener(this, dbTask -> {
                    setRegistrationBusy(false);
                    if (dbTask.isSuccessful()) {
                        Toast.makeText(this,
                                "Registered successfully in Database! You can now log in.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String msg = safeError(dbTask.getException());
                        if (msg.contains("Permission denied")) {
                            msg = "Permission denied: Please enable Email/Password in Firebase Console > Auth, OR set Database Rules to '.read': true, '.write': true in Realtime Database > Rules.";
                        }
                        Toast.makeText(this,
                                "Registration failed: " + msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setRegistrationBusy(boolean busy) {
        btnCreateAccount.setEnabled(!busy);
        btnBackToLogin.setEnabled(!busy);
        btnCreateAccount.setText(busy ? "CREATING ACCOUNT..." : "REGISTER");
    }

    private String safeError(Exception error) {
        return error == null ? "Please try again." : error.getMessage();
    }
}
