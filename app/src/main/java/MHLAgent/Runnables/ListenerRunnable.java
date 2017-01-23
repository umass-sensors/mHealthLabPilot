package MHLAgent.Runnables;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.example.erikrisinger.experiment.MHLAgentService;

import java.io.BufferedReader;
import java.io.IOException;

import MHLAgent.Agent;

/**
 * Created by erisinger on 1/19/17.
 */

public class ListenerRunnable implements Runnable {

    BufferedReader in;
    Agent parent;

    public ListenerRunnable(BufferedReader in, Agent parent) {
        this.in = in;
        this.parent = parent;
    }

    @Override
    public void run() {
        String serverMessage;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                while ((serverMessage = in.readLine()) != null) {

                    //disregard heartbeats from the server
                    if (serverMessage.length() < 2) continue;

                    //transmit message via local broadcast manager
                    Intent messageIntent = new Intent(MHLAgentService.FROM_AGENT_MESSAGE);
                    messageIntent.putExtra("message", serverMessage);

                    parent.getLocalBroadcastManager().sendBroadcast(messageIntent);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
