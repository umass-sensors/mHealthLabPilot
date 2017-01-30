package com.example.erikrisinger.experiment;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import MHLAgent.Agent;

import static MHLAgent.Agent.MESSAGE_JSON_STRING;
import static MHLAgent.Agent.MESSAGE_DATA;
import static MHLAgent.Agent.TO_AGENT_MESSAGE;

/**
 * Created by erikrisinger on 1/17/17.
 */

public class MHLAgentService extends Service {
    Agent agent;
    Thread agentThread;
    LocalBroadcastManager localBroadcastManager;
    Thread subAgent;
    String badgeID = null;
    String ip = "none.cs.umass.edu";
    int port = 9797;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == Agent.DISCONNECT) {
                if (agent != null) {
                    agent.disconnect();
                }
                stopSelf();
            }
        }
    };

    public int onStartCommand(Intent intent, int flags, int startId) {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(Agent.DISCONNECT));
        this.badgeID = intent.getStringExtra(Agent.BADGE_ID);
        new Thread(new SubAgentRunnable()).start();
        return START_STICKY;
    }

    private class SubAgentRunnable implements Runnable{

        @Override
        public void run() {
            localBroadcastManager = LocalBroadcastManager.getInstance(MHLAgentService.this);

            Log.d("agent", "starting up...");

            if (badgeID == null) {
                System.out.println("no badge ID");
                Toast toast = Toast.makeText(MHLAgentService.this, "Set badge ID in settings", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            agent = new Agent(badgeID, ip, port, MHLAgentService.this);;
            new Thread(agent).start();

            System.out.println("SubAgentRunnable: returning");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (agent != null) {
            agent.disconnect();
        }

        stopSelf();
        Log.d("agent", "onDestroy");

    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
