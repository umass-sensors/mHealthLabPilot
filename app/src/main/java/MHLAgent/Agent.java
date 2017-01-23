package MHLAgent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.erikrisinger.experiment.MHLAgentService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import MHLAgent.Messages.Message;
import MHLAgent.Runnables.ListenerRunnable;
import MHLAgent.Runnables.TransmissionRunnable;

/**
 * Created by erisinger on 1/18/17.
 */

public class Agent extends BroadcastReceiver implements Runnable {
    BlockingQueue<Message> queue;

    Thread transmissionThread, listeningThread;
    BufferedReader in;
    BufferedWriter out;
    Socket socket;

    String badgeID;
    String ip;
    int port;
    LocalBroadcastManager localBroadcastManager;

    public Agent(String badgeID, String ip, int port, LocalBroadcastManager localBroadcastManager) {
        queue = new LinkedBlockingQueue<Message>();
        this.badgeID = badgeID;
        this.ip = ip;
        this.port = port;
        this.localBroadcastManager = localBroadcastManager;
        localBroadcastManager.registerReceiver(this, new IntentFilter(MHLAgentService.TO_AGENT_MESSAGE));
    }

    public void run() {

        //connect to the back end and enter the transmission loop
        connectToMHL();
        loop();

    }

    private void loop() {
        Message message;

        try {
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    //dequeue message
                    message = queue.take();

                    //TODO: add transmission timestamp (optional)


                    //transmit to the back end
                    out.write(message.toJSONString() + "\n");
                    out.flush();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToMHL() {
        boolean connected = false;

        while (!Thread.currentThread().isInterrupted() && !connected) {

            //try connecting to the back end and launching threads
            try {
                socket = new Socket(ip, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                connected = true;

                //hand shake
                JSONObject prompt;
                try {
                    prompt = new JSONObject(in.readLine());
                    prompt.put("type", "shibboleth");
                    prompt.put("badge-id", badgeID);
                    prompt.put("process-type", "field-device");

                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                out.write(prompt.toString() + "\n");
                out.flush();

                JSONObject ack;
                try {
                    ack = new JSONObject(in.readLine());
                    Intent intent = new Intent(MHLAgentService.TO_AGENT_MESSAGE);
                    intent.putExtra(MHLAgentService.MESSAGE_DATA, ack.toString());
                    localBroadcastManager.sendBroadcast(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                //launch IO threads
                transmissionThread = new Thread(new TransmissionRunnable(queue, out, this));
                transmissionThread.start();

                listeningThread = new Thread(new ListenerRunnable(in, this));
                listeningThread.start();

            } catch (IOException e) {
                try {
                    System.out.print("unable to connect, retrying...");
                    Thread.sleep(2000);
                } catch (InterruptedException f) {
                    f.printStackTrace();
                }
            }
        }

        System.out.print("connected");
    }

    public void addMessage(Message message) {

        if (!queue.offer(message)) {
            //TODO: if enqueuing fails, write data to disk

        }
    }

    private void interruptThreads() {
        System.out.print("interrupting threads...");
        transmissionThread.interrupt();
        listeningThread.interrupt();
    }

    public LocalBroadcastManager getLocalBroadcastManager() {
        return localBroadcastManager;
    }

    public void onLostConnection() {
        System.out.print("attempting to reconnect...");
        connectToMHL();
    }

    public void disconnect() {
        Thread.currentThread().interrupt();
        interruptThreads();

        //TODO: unregister broadcast listeners

    }

    public void onReceive(Context context, Intent intent) {
        //TODO: pretty much anything would be good

    }
}
