package MHLAgent.Runnables;

import java.io.BufferedReader;
import java.io.IOException;

import MHLAgent.Agent;

/**
 * Created by erisinger on 1/19/17.
 */

public class ListenerRunnable implements Runnable {

    private BufferedReader in;
    private Agent parent;

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
                    if (serverMessage.length() < 2) {
                        continue;
                    }

                    parent.onReceivedMessageFromBackEnd(serverMessage);
                }
            }
        }catch(IOException e){
//            e.printStackTrace();
//            parent.onLostConnection();
        }finally{
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("cleaned up listener resources");
        }
    }
}
