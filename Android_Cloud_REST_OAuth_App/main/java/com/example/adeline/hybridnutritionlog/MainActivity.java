package com.example.adeline.hybridnutritionlog;

// MainActivity.java
// Author: Adeline Harcourt based on skeleton code from Professor Justin Wolford in CS 496.
// Description: The main activity file for a simple app that utilizes a cloud-based nutritional
// log API in order to allow users to save nutritional logs.

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.icu.util.Calendar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.app.PendingIntent;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import okhttp3.HttpUrl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;

public class MainActivity extends AppCompatActivity {
    private AuthorizationService mAuthorizationService;
    private AuthState mAuthState;
    private OkHttpClient mOkHttpClient;
    protected String googleID = "";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuthorizationService = new AuthorizationService(this);

        // Set On Click Listener for sumbit log button
        ((Button)findViewById(R.id.main_submit)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(isFormFilled()){
                    submitForm();
                }
                else{
                    // Display alert if the form fields are not properly filled out
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(MainActivity.this);
                    dlgAlert.setMessage("Please fill in all form fields");
                    dlgAlert.setTitle("Empty Fields");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(false);
                    dlgAlert.create().show();
                }
            }
        });
    }

    // Overridden onStart method. Updates the Authorization State and gets the Google ID
    @Override
    protected void onStart() {
        mAuthState = this.getOrCreateAuthState();
        getGoogleID();
        super.onStart();
    }

    // Checks if the form fields are correctly filled out. Returns true if they are, false if not.
    boolean isFormFilled() {
        if (((EditText)findViewById(R.id.main_date)).getText().toString().matches("")) {
            return false;
        }
        else if (((EditText)findViewById(R.id.main_calories)).getText().toString().matches("")) {
            return false;
        }
        else if (((EditText)findViewById(R.id.main_weight)).getText().toString().matches("")) {
            return false;
        }
        else if (!((RadioButton)findViewById(R.id.main_exercise_T)).isChecked() && !((RadioButton)findViewById(R.id.main_exercise_F)).isChecked()) {
            return false;
        }
        else {
            return true;
        }
    }

    // Gets the Google ID of the user.
    private void getGoogleID() {
        try {
            mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
                    if (e == null) {
                        mOkHttpClient = new OkHttpClient();
                        HttpUrl reqUrl = HttpUrl.parse("https://www.googleapis.com/plus/v1/people/me");
                        reqUrl = reqUrl.newBuilder().addQueryParameter("key", "AIzaSyAHExAKBWJLJRgI0K7tivhdcZeh6CO67rU").build();

                        // GET from Google+
                        Request request = new Request.Builder()
                                .url(reqUrl)
                                .addHeader("Authorization", "Bearer " + accessToken)
                                .build();

                        mOkHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                            }

                            // Process response
                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try {
                                    String r = response.body().string();            // Response string
                                    JSONObject j = new JSONObject(r);               // JSON response
                                    googleID = j.getString("id");
                                    showLogs();

                                } catch (JSONException el) {
                                    el.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Submits the log form for the user. Verifies that the date entered is not a duplicate of one already in the database.
    private void submitForm() {
        final String dateText = ((EditText)findViewById(R.id.main_date)).getText().toString();
        final int calories = Integer.valueOf(((EditText)findViewById(R.id.main_calories)).getText().toString());
        final double weight = Double.valueOf(((EditText)findViewById(R.id.main_weight)).getText().toString());
        final boolean exercised = ((RadioButton)findViewById(R.id.main_exercise_T)).isChecked();

        try {
            mOkHttpClient = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs");
            reqUrl = reqUrl.newBuilder().addQueryParameter("date", dateText).build();

            // GET GoogleID
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .addHeader("user", googleID)
                    .build();

            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                // Process response
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String r = response.body().string();            // Response string
                        if (r.contains("{")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder dateAlert = new AlertDialog.Builder(MainActivity.this);
                                    dateAlert.setMessage("Whoops! You already made a log for that date.");
                                    dateAlert.setTitle("Duplicate date");
                                    dateAlert.setPositiveButton("OK", null);
                                    dateAlert.setCancelable(false);
                                    dateAlert.create().show();
                                }
                            });
                        }
                        else {
                            postLog(dateText, calories, weight, exercised);
                        }
                    } catch (Exception el) {
                        el.printStackTrace();
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Posts the user's log to the cloud database.
    private void postLog(final String dateText, final int calories, final double weight, final boolean exercised){
        try {
            mOkHttpClient = new OkHttpClient();
            String json = "{\"user\": \"" + googleID + "\", \"date\": \"" + dateText + "\", \"calories\": " + calories + ", \"weight\": " + weight + ", \"exercise\": " + exercised + "}";
            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs");

            // POST to nutrition log API
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .post(body)
                    .build();

            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String r = response.body().string();
                    showLogs();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Displays the user's logs in a Listview
    private void showLogs(){
        try {
            mOkHttpClient = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs");

            // GET all user's posts
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .addHeader("user", googleID)
                    .build();

            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                // Process response
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String r = response.body().string();            // Response string
                        JSONArray j = new JSONArray(r);               // JSON response
                        List<Map<String, String>> logs = new ArrayList<Map<String, String>>();     // ArrayList of posts

                        for (int i = 0; i < j.length(); i++) {
                            HashMap<String, String> m = new HashMap<String, String>();
                            m.put("date", j.getJSONObject(i).getString("date"));
                            m.put("calories", j.getJSONObject(i).getString("calories"));
                            m.put("weight", j.getJSONObject(i).getString("weight"));
                            m.put("weight", j.getJSONObject(i).getString("weight"));
                            String exercised = j.getJSONObject(i).getString("exercise");
                            if (exercised.equals("true")) {
                                m.put("exercise", "Yes");
                            }
                            else {
                                m.put("exercise", "No");
                            }
                            m.put("id", j.getJSONObject(i).getString("id"));
                            logs.add(m);
                        }

                        // Create adapter to add logs to listview
                        final SimpleAdapter logAdapter = new SimpleAdapter (
                                MainActivity.this,
                                logs,
                                R.layout.activity_log_list_layout,
                                new String[]{"date", "calories", "weight", "exercise", "id"},
                                new int[]{R.id.log_date, R.id.log_calories, R.id.log_weight, R.id.log_exercise, R.id.log_id});

                        // Set adapter
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ListView) findViewById(R.id.main_LogList)).setAdapter(logAdapter);
                            }
                        });

                        // Set onclicklistener for all rows in the listview
                        final ListView list = (ListView) findViewById(R.id.main_LogList);
                        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                                HashMap<String, Object> obj = (HashMap<String, Object>) logAdapter.getItem(position);
                                String rowID = (String) obj.get("id");

                                Intent intent = new Intent(MainActivity.this, EditLogActivity.class);
                                intent.putExtra("ID", rowID);
                                intent.putExtra("googleID", googleID);
                                startActivity(intent);
                            }
                        });
                    } catch (JSONException el) {
                        el.printStackTrace();
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Source: https://developer.android.com/guide/topics/ui/controls/pickers.html
    public void showDatePickerDialog(View v) {
        TextView dateText = (TextView)findViewById(R.id.main_date);
        DialogFragment newFragment = new DatePickerFragment(dateText);
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }


    // Source: https://developer.android.com/guide/topics/ui/controls/pickers.html
    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

            TextView dateTextView;

            public DatePickerFragment(TextView textview)
            {
                dateTextView = textview;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                // Use the current date as the default date in the picker
                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                // Create a new instance of DatePickerDialog and return it
                return new DatePickerDialog(getActivity(), this, year, month, day);
            }

            public void onDateSet(DatePicker view, int year, int month, int day) {
                dateTextView.setText(String.valueOf(month+1)+"-"+String.valueOf(day)+"-"+String.valueOf(year));
            }
    }

    // Checks to see if authorization state is valid. If not, calls updateAuthState()
    AuthState getOrCreateAuthState(){
        AuthState auth = null;
        SharedPreferences authPreference = getSharedPreferences("auth", MODE_PRIVATE);
        String stateJson = authPreference.getString("stateJson", null);

        if(stateJson != null){
            try {
                auth = AuthState.jsonDeserialize(stateJson);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        if(auth != null && auth.getAccessToken() != null){
            return auth;
        } else {
            updateAuthState();
            return null;
        }
    }

    // Updates the authorization state by sending a request to the Google+ Domains API.
    void updateAuthState() {
        Uri authEndpoint = new Uri.Builder().scheme("https").authority("accounts.google.com").path("/o/oauth2/v2/auth").build();
        Uri tokenEndpoint = new Uri.Builder().scheme("https").authority("www.googleapis.com").path("/oauth2/v4/token").build();
        Uri redirect = new Uri.Builder().scheme("com.example.adeline.hybridnutritionlog").path("foo").build();

        // Build request to send to API
        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint, null);
        AuthorizationRequest req = new AuthorizationRequest.Builder(config, "830389502526-qeu4tg2ik6h2grsnac3d7rfc3km80g2v.apps.googleusercontent.com", ResponseTypeValues.CODE, redirect)
                .setScopes("https://www.googleapis.com/auth/plus.me")
                .build();

        // Create new pending intent to redirect return from request
        Intent authComplete = new Intent(this, AuthCompleteActivity.class);
        mAuthorizationService.performAuthorizationRequest(req, PendingIntent.getActivity(this, req.hashCode(), authComplete, 0));
    }
}