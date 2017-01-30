package MHLAgent.Runnables;

import android.app.Activity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

import MHLAgent.Agent;
import MHLAgent.Messages.Message;

/**
 * Created by erisinger on 1/19/17.
 */

public class TransmissionRunnable implements Runnable {
    private BlockingQueue<String> queue;
    private BufferedWriter out;
    private Agent parent;

    public TransmissionRunnable(BlockingQueue<String> queue, BufferedWriter out, Agent parent) {
        this.queue = queue;
        this.out = out;
        this.parent = parent;
    }

    @Override
    public void run() {

//        System.out.println("TransmissionRunnable.run()");

        try {
            String message;

            //enter transmission loop
            while (!Thread.currentThread().isInterrupted()) {

//                System.out.println("TransmissionRunnable.run(): queue size: " + queue.size());

                try {
                    message = queue.take();
                    out.write(message + "\n");
                    out.flush();

//                    System.out.println("TransmissionRunnable.run(): transmitted message");

                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }

            System.out.println("TransmissionRunnable.run(): interrupted");

        } catch (IOException e) {
            System.out.print("connection lost (transmission)...");
            parent.onLostConnection();
        } finally {

            System.out.print("cleaning up transmission thread...");
            //clean up
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.print("complete...");
        }

        System.out.println("TransmissionRunnable.run(): exiting");

    }
}
