package com.example.erikrisinger.experiment;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.SampleRate;

import MHLAgent.Agent;
import MHLAgent.Messages.AccelerometerSensorMessage;
import MHLAgent.Messages.GyroscopeSensorMessage;
import MHLAgent.Messages.HeartRateSensorMessage;
import MHLAgent.Messages.Message;

/**
 * Created by erisinger on 1/24/17.
 */

public class BandProxyService extends Service implements BandAccelerometerEventListener, BandGyroscopeEventListener, BandHeartRateEventListener, BandGsrEventListener {

    LocalBroadcastManager broadcastManager;
    BandClient bandClient;
    String badgeID;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {

        System.out.println("BandProxyService started");

        if (intent.hasExtra(Agent.BADGE_ID)) {
            this.badgeID = intent.getStringExtra(Agent.BADGE_ID);
        } else {
            System.out.println("no badge ID available!");
        }

        broadcastManager = LocalBroadcastManager.getInstance(this);

        new SensorSubscriptionTask().execute();

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Agent.DISCONNECT)) {
                    System.out.println("band service received disconnect message");
//                    (new AsyncTask<Void, Void, Void>() {
//                        @Override
//                        protected Void doInBackground(Void... params) {
//                            disconnectFromBand();
//                            return null;
//                        }
//                    }).execute();
                    disconnectFromBand();
                    stopSelf();
                }
            }
        };

        broadcastManager.registerReceiver(broadcastReceiver, new IntentFilter(Agent.DISCONNECT));

        return START_STICKY;
    }

    @Override
    public void onBandAccelerometerChanged(BandAccelerometerEvent e) {
        broadcastSensorMessage(new AccelerometerSensorMessage(badgeID, e.getTimestamp(), e.getAccelerationX(), e.getAccelerationY(), e.getAccelerationZ()));
    }

    @Override
    public void onBandGyroscopeChanged(BandGyroscopeEvent e) {
        broadcastSensorMessage(new GyroscopeSensorMessage(badgeID, e.getTimestamp(), e.getAngularVelocityX(), e.getAngularVelocityY(), e.getAngularVelocityZ()));
    }

    @Override
    public void onBandHeartRateChanged(BandHeartRateEvent e) {
        broadcastSensorMessage(new HeartRateSensorMessage(badgeID, e.getTimestamp(), e.getHeartRate()));
    }

    @Override
    public void onBandGsrChanged(BandGsrEvent e) {
        //TODO: implement GSR message subclass, broadcast

    }

    private void broadcastSensorMessage(Message message) {
        Intent intent = new Intent(Agent.SENSOR_MESSAGE);
        intent.putExtra(Agent.MESSAGE_JSON_STRING, message.toJSONString());
        broadcastManager.sendBroadcast(intent);
    }

    private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerAccelerometerEventListener(BandProxyService.this, SampleRate.MS32);
                    bandClient.getSensorManager().registerGyroscopeEventListener(BandProxyService.this, SampleRate.MS32);
                    bandClient.getSensorManager().registerHeartRateEventListener(BandProxyService.this);
                } else {
                    System.out.println("couldn't connect to band!");
                }
            } catch (BandException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
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
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    private void disconnectFromBand() {

//        System.out.println("attempting to disconnect from band");

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
    public IBinder onBind(Intent intent) {
        return null;
    }
}
