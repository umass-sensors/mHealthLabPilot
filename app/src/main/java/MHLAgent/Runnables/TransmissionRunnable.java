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
    BlockingQueue<Message> queue;
    BufferedWriter out;
    Agent parent;

    public TransmissionRunnable(BlockingQueue<Message> queue, BufferedWriter out, Agent parent) {
        this.queue = queue;
        this.out = out;
        this.parent = parent;
    }

    @Override
    public void run() {

        try {
            Message message;

            //enter transmission loop
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    message = queue.take();
                    out.write(message.toJSONString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.out.print("connection lost...");
            parent.onLostConnection();
        } finally {

            //clean up
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
