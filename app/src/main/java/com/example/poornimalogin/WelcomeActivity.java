package com.example.poornimalogin;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView tvWelcome = findViewById(R.id.tvWelcome);

        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = "Student";
        }

        tvWelcome.setText("Welcome, " + username + "!");
    }
}
