package com.example.erikrisinger.experiment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;

import MHLAgent.Agent;

public class MainActivity extends AppCompatActivity {

    LocalBroadcastManager broadcastManager;
    BandClient bandClient;

    public static final String PREFS_FILE = "prefs";
    public static final String BADGE_ID = "badge-id";
    public static final String START_BUTTON_TEXT = "start-button-text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: check shared preferences for user ID -- if none, launch settings
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_FILE, 0);
        if (sharedPreferences.contains(START_BUTTON_TEXT)) {
            Button startButton = (Button) findViewById(R.id.start_stop_button);
            startButton.setText(sharedPreferences.getString(START_BUTTON_TEXT, null));
        }


        if (!sharedPreferences.contains(BADGE_ID)) {
            //TODO: launch settings
            startActivity(new Intent(this, SettingsActivity.class));
        }

        broadcastManager = LocalBroadcastManager.getInstance(this);
//        broadcastManager.registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.d("main", intent.getAction());
//
//            }
//        }, new IntentFilter(Agent.MESSAGE_JSON_STRING));

        Log.d("main", "stopping service");


        (new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                try {
                    getConnectedBandClient();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (bandClient.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED) {
                    bandClient.getSensorManager().requestHeartRateConsent(MainActivity.this, heartRateConsentListener);
                }

                return null;
            }

        }).execute();
    }

    public void startStop(View view) {
        System.out.println("startStop");

        String id;
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_FILE, 0);
        if (sharedPreferences.contains(BADGE_ID)) {
            id = sharedPreferences.getString(BADGE_ID, null);
        } else {
            System.out.println("no badge ID set!");
            //TODO: launch settings, make toast, etc.
            Toast toast = Toast.makeText(this, "Set badge ID in settings before starting", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Button button = (Button)findViewById(R.id.start_stop_button);
        if (button.getText().equals(getResources().getString(R.string.start))) {

            //set extras and start services
            Intent agentServiceIntent = new Intent(this, MHLAgentService.class);
            agentServiceIntent.putExtra(Agent.BADGE_ID, id);
            startService(agentServiceIntent);

            Intent bandServiceIntent = new Intent(this, BandProxyService.class);
            bandServiceIntent.putExtra(Agent.BADGE_ID, id);
            startService(bandServiceIntent);
            button.setText(getResources().getString(R.string.stop));
        } else {
            broadcastManager.sendBroadcast(new Intent(Agent.DISCONNECT));
//            stopService(new Intent(this, BandProxyService.class));
//            stopService(new Intent(this, MHLAgentService.class));
            button.setText(getResources().getString(R.string.start));
        }
    }

    public void openSettings(View view) {
        System.out.println("openSettings");
        Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(openSettingsIntent);
    }

    public void viewSurveys(View view) {
        if (((Button)findViewById(R.id.start_stop_button)).getText().equals("Start")) {
            Toast toast = Toast.makeText(this, "Press start to connect to server before retrieving surveys!", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        System.out.println("viewSurveys");
        Intent viewSurveysIntent = new Intent(this, SurveysActivity.class);
        startActivity(viewSurveysIntent);
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                return false;
            }
            bandClient = BandClientManager.getInstance().create(this, devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            return true;
        }

        if (bandClient.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED) {
            bandClient.getSensorManager().requestHeartRateConsent(this, heartRateConsentListener);
        }

        //TODO: this is not correct...
//        bandClient.connect();
//        return true;
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    public HeartRateConsentListener heartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {

        }
    };

    private void disconnectFromBand() {

        System.out.println("attempting to disconnect from band");

        //unregister sensors and disconnect band
        if (bandClient != null) {
            try {
                System.out.println("trying to disconnect from band...");
                bandClient.getSensorManager().unregisterAllListeners();
                bandClient.disconnect().await();
            } catch (InterruptedException | BandException e) {
//                e.printStackTrace();
            }
            System.out.println("disconnected");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_FILE, 0);
        if (sharedPreferences.contains(START_BUTTON_TEXT)) {
            Button startButton = (Button) findViewById(R.id.start_stop_button);
            startButton.setText(sharedPreferences.getString(START_BUTTON_TEXT, null));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_FILE, 0);
        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putString(START_BUTTON_TEXT, ((Button)findViewById(R.id.start_stop_button)).getText().toString());
        ed.commit();

        disconnectFromBand();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
