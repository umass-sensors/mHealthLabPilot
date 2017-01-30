package MHLAgent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.example.erikrisinger.experiment.MHLAgentService;
import com.example.erikrisinger.experiment.MainActivity;
import com.example.erikrisinger.experiment.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import MHLAgent.Runnables.ListenerRunnable;
import MHLAgent.Runnables.TransmissionRunnable;

/**
 * Created by erisinger on 1/18/17.
 */

public class Agent implements Runnable {
    private BlockingQueue<String> queue;
    private Thread transmissionThread, listeningThread;
    private BufferedReader in;
    private BufferedWriter out;
    private Socket socket;

    private String badgeID;
    private String ip;
    private int port;
    private LocalBroadcastManager localBroadcastManager;
    private MHLAgentService service;

    public ArrayList<JSONObject> surveys;

    public static final String TO_AGENT_MESSAGE = "to-agent";
    public static final String FROM_AGENT_MESSAGE = "from-agent";
    public static final String MESSAGE_DATA = "data";
    public static final String MESSAGE_JSON_STRING = "message";
    public static final String DISCONNECT = "disconnect";
    public static final String SENSOR_MESSAGE = "sensor-message";
    public static final String BADGE_ID = "badge-id";
    public static final String FROM_BACKEND_MESSAGE = "from-backend-message";
    public static final String RETRIEVE_ALL_SURVEYS = "retrieve-surveys";
    public static final String SEND_ALL_SURVEYS = "send-surveys";
    public static final String SURVEYS_LIST = "surveys-list";
    public static final String REMOVE_SURVEY = "remove-survey";
    public static final String INDEX = "index";

    public Agent(String badgeID, String ip, int port, MHLAgentService service) {
        queue = new LinkedBlockingQueue<String>();
        this.badgeID = badgeID;
        this.ip = ip;
        this.port = port;
        this.service = service;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(service);
        this.surveys = new ArrayList<>();

        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(TO_AGENT_MESSAGE));
        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(SENSOR_MESSAGE));
        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(DISCONNECT));
        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(RETRIEVE_ALL_SURVEYS));
        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(REMOVE_SURVEY));
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//            System.out.println("received message with action: " + intent.getAction());

            String action = intent.getAction();
            if (action.equals(DISCONNECT)) {
                disconnect();
            } else  if (action.equals(SENSOR_MESSAGE)) {
                addMessage(intent.getStringExtra(MESSAGE_JSON_STRING));
            } else if (action.equals(REMOVE_SURVEY)) {
                int index = intent.getIntExtra(INDEX, -1);
                if (index == -1) {
                    //somebody forgot to add the removal index...

                } else {
                    surveys.remove(index);
                }
            } else if (action.equals(RETRIEVE_ALL_SURVEYS)) {

                System.out.println("Agent.onReceive() received survey retrieval intent");

                //request for all surveys -- check expiration, package into list and ship out
                ArrayList<String> stringSurveys = new ArrayList<>();
                for (JSONObject o : surveys) {

                    try {
                        JSONObject header = (JSONObject) o.get("header");
//                        long time = System.currentTimeMillis();
//                        long exp = (long)header.get("expires-at");
//                        long dur = (long)header.get("expires-after");
//                        long rec = (long)header.get("received-at");

//                        if ((exp > time || exp == 0) && rec + dur > time) {
                            stringSurveys.add(o.toString());
//                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Intent surveysIntent = new Intent(SEND_ALL_SURVEYS);
                surveysIntent.putStringArrayListExtra(SURVEYS_LIST, stringSurveys);

                localBroadcastManager.sendBroadcast(surveysIntent);
            }
        }
    };

    public void run() {

        //temporary test for surveys activity
        for (int i = 0; i < 5; i++) {
            try {
                surveys.add(new JSONObject(service.getResources().getString(R.string.survey_JSON_escaped)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //connect to the back end and launch communication threads
        connectToMHL();
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
                    Intent intent = new Intent(TO_AGENT_MESSAGE);
                    intent.putExtra(MESSAGE_DATA, ack.toString());
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

    public void addMessage(String message) {

//        System.out.println("Agent.addMessage(): " + message);

        if (!queue.offer(message)) {
            //TODO: if enqueuing fails, write data to disk

        }
    }

    public void onReceivedMessageFromBackEnd(String messageString) {

        try {
            JSONObject message = new JSONObject(messageString);
            JSONObject header = (JSONObject)message.get("header");
            JSONObject metadata = (JSONObject)message.get("metadata");
            JSONObject payload = (JSONObject)message.get("payload");

            String messageType = (String)header.get("message-type");

            //determine type of message
            if (messageType.equals("response-request")) {
                if (((String)metadata.get("request-type")).equals("survey")) {

                    //make sure survey hasn't already expired
                    long time = System.currentTimeMillis();
                    long expires = (long)header.get("expires-at");
                    if (expires > time || expires == 0) {
                        System.out.println("survey expired");
                        return;
                    }

                    //mark survey receipt time
                    header.put("received-at", time);
                    message.put("header", header);
                    surveys.add(message);
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void interruptThreads() {
        System.out.print("interrupting threads...");
        transmissionThread.interrupt();
        listeningThread.interrupt();

        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    public void onLostConnection() {
        System.out.print("attempting to reconnect...");
        try {
            socket.close();
        } catch (IOException e) {

        }
        connectToMHL();
    }

    public void disconnect() {
        Thread.currentThread().interrupt();
        interruptThreads();
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
        try {
            socket.close();
        } catch (IOException e) {

        }

        SharedPreferences prefs = service.getSharedPreferences(MainActivity.PREFS_FILE, 0);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(MainActivity.START_BUTTON_TEXT, service.getResources().getString(R.string.start));
        ed.commit();
    }

}
