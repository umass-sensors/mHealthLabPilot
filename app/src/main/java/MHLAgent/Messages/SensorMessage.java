package MHLAgent.Messages;

import org.json.JSONObject;

/**
 * Created by erisinger on 1/23/17.
 */

public abstract class SensorMessage extends Message {

    public SensorMessage(String badgeID, String channel, long firstTimestamp) {
        super(badgeID, channel, "sensor-message", firstTimestamp);
    }
}
