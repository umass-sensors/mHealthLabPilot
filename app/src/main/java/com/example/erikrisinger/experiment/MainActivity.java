package com.example.erikrisinger.experiment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService(new Intent(this, MHLAgentService.class));

        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("main", intent.getAction());
                broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
            }
        }, new IntentFilter(MHLAgentService.FROM_AGENT_MESSAGE));

        Log.d("main", "starting service");

//        startService(new Intent(this, MHLAgentService.class));

//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));

        Log.d("main", "stopping service");

        stopService(new Intent(this, MHLAgentService.class));

//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
//        broadcastManager.sendBroadcast(new Intent(MHLAgentService.TO_AGENT_MESSAGE));
    }
}
