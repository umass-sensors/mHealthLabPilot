package com.example.erikrisinger.experiment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_FILE, 0);
        if (sharedPreferences.contains("badge-id")) {
            EditText editText = (EditText)findViewById(R.id.badge_id_edit_text);
            editText.setText(sharedPreferences.getString("badge-id", "Badge ID"));
        }
    }

    public void saveSettings(View view) {
        //TODO: save settings to shared prefs, return to main activity
        EditText editText = (EditText)findViewById(R.id.badge_id_edit_text);
        String id = editText.getText().toString();

        //commit badge ID
        if (id.length() > 1) {
            SharedPreferences sharedPreferences = getSharedPreferences(MainActivity.PREFS_FILE, 0);
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putString("badge-id", id);
            ed.commit();

            //return to main activity
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}
