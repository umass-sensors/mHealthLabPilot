package com.example.erikrisinger.experiment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import MHLAgent.Agent;

public class SurveysActivity extends AppCompatActivity {

    ArrayList<JSONObject> surveys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surveys);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Agent.SEND_ALL_SURVEYS)) {
                    surveys = getAllSurveys(intent.getStringArrayListExtra(Agent.SURVEYS_LIST));

                    System.out.println("retrieved " + surveys.size() + " surveys");

                    displaySurveys();
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Agent.SEND_ALL_SURVEYS));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Agent.RETRIEVE_ALL_SURVEYS));

        System.out.println("sent broadcast to request surveys");
    }

    public ArrayList<JSONObject> getAllSurveys(ArrayList<String> stringList) {
        ArrayList<JSONObject> jsonList = new ArrayList<>();

        for (String s : stringList) {
            try {
                jsonList.add(new JSONObject(s));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonList;
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int i = (int)view.getTag();
            JSONObject o = surveys.get(i);

            //TODO: replace the following toast with DCS' implementation -- startActivityForResult()...
            Toast toast = Toast.makeText(SurveysActivity.this, surveys.get(i).toString(), Toast.LENGTH_SHORT);
            toast.show();
            
            Intent removeSurveyIntent = new Intent(Agent.REMOVE_SURVEY);
            removeSurveyIntent.putExtra(Agent.INDEX, i);
            LocalBroadcastManager.getInstance(SurveysActivity.this).sendBroadcast(removeSurveyIntent);
            LocalBroadcastManager.getInstance(SurveysActivity.this).sendBroadcast(new Intent(Agent.RETRIEVE_ALL_SURVEYS));
            displaySurveys();
        }
    };

    private void displaySurveys() {
        //TODO: add a button (or something) for each survey that launches DCS's survey activity
        LinearLayout layout = (LinearLayout)findViewById(R.id.surveys_layout);
        layout.removeAllViews();
        JSONObject metadata = null;
        String title = "take survey";
        int tag = 0;
        for (JSONObject o : surveys) {
            try {
                metadata = (JSONObject) o.get("metadata");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (metadata != null) {
                try {
                    //TODO: add title field to survey primitive
                    if (metadata.has("survey-title")) {
                        title = metadata.getString("survey-title");
                    } else {
                        title = "Survey " + tag;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Button button = new Button(this);
            button.setText(title);
            button.setTag(tag);
            tag++;
            button.setOnClickListener(listener);

            layout.addView(button);
        }

    }
}
