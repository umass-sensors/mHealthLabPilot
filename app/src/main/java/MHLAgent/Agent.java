package MHLAgent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import MHLAgent.Messages.MHLMessage;
import MHLAgent.Runnables.ListenerRunnable;
import MHLAgent.Runnables.TransmissionRunnable;

/**
 * Created by erisinger on 1/18/17.
 */

public class Agent implements Runnable {
    Thread transmissionThread, listeningThread;
    BlockingQueue<MHLMessage> queue;
    String badgeID;
    String ip;

    public Agent(String badgeID, String ip) {
        queue = new LinkedBlockingQueue<MHLMessage>();
        this.badgeID = badgeID;
        this.ip = ip;
    }

    public void run() {
        connectToMHL();
    }

    private void connectToMHL() {
        transmissionThread = new Thread(new TransmissionRunnable());
        transmissionThread.start();

        listeningThread = new Thread(new ListenerRunnable());
        listeningThread.start();
    }

    public void addMessage(MHLMessage message) {
        boolean queued = queue.offer(message);

        //TODO: if enqueuing fails, write data to disk
        
    }

}
