package com.example.myapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ProfileSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_selection);
    }

    public void onLogin(View view) {
        EditText textField = findViewById(R.id.editTextUsername);
        final String username = textField.getText().toString();
        if(username.length() == 0) {
            Toast.makeText(this, "Please specify a username", Toast.LENGTH_SHORT).show();
            return;
        }

        (new File(this.getExternalFilesDir(null), username+"/pictures")).mkdirs();
        (new File(this.getExternalFilesDir(null), username+"/videos")).mkdirs();

        Intent intent = new Intent(this, ChannelsActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }
}