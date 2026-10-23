package com.example.poornimalogin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private DatabaseReference usersRef, profilesRef;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        FirebaseDatabase database;
        try {
            database = FirebaseDatabase.getInstance();
        } catch (Exception e) {
            database = FirebaseDatabase.getInstance("https://poornimalogin-default-rtdb.firebaseio.com/");
        }

        usersRef = database.getReference("users");      // Existing demo accounts
        profilesRef = database.getReference("profiles"); // New Auth account profiles
        firebaseAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void loginUser() {
        String identifier = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter your email/username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setLoginBusy(true);
        if (identifier.contains("@")) {
            // Accounts created through the registration page: Firebase Authentication.
            loginRegisteredUser(identifier, password);
        } else {
            // Preserve the original username/password demo workflow for old accounts.
            loginLegacyUser(identifier, password);
        }
    }

    private void loginRegisteredUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        // If Firebase Auth fails (e.g. CONFIGURATION_NOT_FOUND or DB account),
                        // fall back to checking Realtime Database directly.
                        loginLegacyUser(email, password);
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        setLoginBusy(false);
                        Toast.makeText(this, "Unable to load signed-in user",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fallback = email.substring(0, email.indexOf('@'));
                    profilesRef.child(user.getUid()).child("username").get()
                            .addOnCompleteListener(this, profileTask -> {
                                setLoginBusy(false);
                                String displayName = fallback;
                                if (profileTask.isSuccessful()
                                        && profileTask.getResult() != null) {
                                    String stored = profileTask.getResult()
                                            .getValue(String.class);
                                    if (stored != null && !stored.trim().isEmpty()) {
                                        displayName = stored;
                                    }
                                }
                                openWelcome(displayName);
                            });
                });
    }

    private void loginLegacyUser(String identifier, String password) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                setLoginBusy(false);
                for (DataSnapshot user : snapshot.getChildren()) {
                    String dbUsername = user.child("username").getValue(String.class);
                    String dbEmail = user.child("email").getValue(String.class);
                    Object savedPassword = user.child("password").getValue();
                    String dbPassword = savedPassword == null ? ""
                            : String.valueOf(savedPassword);

                    boolean matchIdentifier = (dbUsername != null && identifier.equalsIgnoreCase(dbUsername))
                            || (dbEmail != null && identifier.equalsIgnoreCase(dbEmail));

                    if (matchIdentifier && password.equals(dbPassword)) {
                        String displayName = (dbUsername != null && !dbUsername.isEmpty())
                                ? dbUsername : identifier;
                        openWelcome(displayName);
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "Invalid Email/Username or Password",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                setLoginBusy(false);
                String msg = error.getMessage();
                if (msg != null && msg.contains("Permission denied")) {
                    msg = "Permission denied: Please update Rules in Firebase Console > Realtime Database > Rules to allow read/write.";
                }
                Toast.makeText(MainActivity.this,
                        "Firebase Error: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openWelcome(String username) {
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    private void setLoginBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnRegister.setEnabled(!busy);
        btnLogin.setText(busy ? "Checking..." : "LOGIN");
    }
}
