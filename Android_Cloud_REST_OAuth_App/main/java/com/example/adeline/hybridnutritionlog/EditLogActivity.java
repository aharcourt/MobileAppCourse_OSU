package com.example.adeline.hybridnutritionlog;

// EditLogActivity.java
// Author: Adeline Harcourt based on skeleton code from Professor Justin Wolford in CS 496.
// Description: The edit log activity file for a simple app log This activity allows users to edit or
// delete logs that they have previously created.

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.icu.util.Calendar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.adeline.hybridnutritionlog.MainActivity.JSON;

public class EditLogActivity extends AppCompatActivity {
    private OkHttpClient mOkHttpClient;
    private String googleID = "";
    private String rowID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_log);

        Bundle bundle = getIntent().getExtras();
        rowID = bundle.getString("ID");
        googleID = bundle.getString("googleID");

        setText();

        // Set OnClickListener for the Delete button
        ((Button)findViewById(R.id.edit_Delete)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                deleteLog();
            }
        });

        // Set OnClickListener for the Save button
        ((Button)findViewById(R.id.edit_SaveChanges)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(isFormFilled()){
                    editLog();
                }
                // Display alert if all form fields are not filled out
                else{
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(EditLogActivity.this);
                    dlgAlert.setMessage("Please fill in all form fields");
                    dlgAlert.setTitle("Empty Fields");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(false);
                    dlgAlert.create().show();
                }
            }
        });

        // Set OnClickListener for the Cancel button
        ((Button)findViewById(R.id.edit_Cancel)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Check that all form fields are filled out. Return true if so, false if not.
    boolean isFormFilled() {
        if (((EditText)findViewById(R.id.edit_date)).getText().toString().matches("")) {
            return false;
        }
        else if (((EditText)findViewById(R.id.edit_calories)).getText().toString().matches("")) {
            return false;
        }
        else if (((EditText)findViewById(R.id.edit_weight)).getText().toString().matches("")) {
            return false;
        }
        else if (!((RadioButton)findViewById(R.id.edit_exercise_T)).isChecked() && !((RadioButton)findViewById(R.id.edit_exercise_F)).isChecked()) {
            return false;
        }
        else {
            return true;
        }
    }

    // Sets the text for the form up. Automatically populates the form fields with values from the
    // row that the user clicked on.
    public void setText(){
        try {
            mOkHttpClient = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs/" + rowID);

            // GET information for row clicked
            Request request = new Request.Builder()
                    .url(reqUrl)
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

                        // Get values from API
                        final String oldDate = j.getString("date");
                        final String calories = j.getString("calories");
                        final String weight = j.getString("weight");
                        final String exercised = j.getString("exercise");

                        // Set textviews to API values
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView tv_date = (TextView)findViewById(R.id.edit_date);
                                tv_date.setText(Integer.valueOf(oldDate.substring(5,7)).toString() + "-" + Integer.valueOf(oldDate.substring(8,10)).toString() + "-" + Integer.valueOf(oldDate.substring(0,4)).toString());

                                TextView tv_calories = (TextView)findViewById(R.id.edit_calories);
                                tv_calories.setText(calories);

                                TextView tv_weight = (TextView)findViewById(R.id.edit_weight);
                                tv_weight.setText(weight);

                                if (exercised.equals("true")) {
                                    RadioButton ex_T = (RadioButton)findViewById(R.id.edit_exercise_T);
                                    ex_T.setChecked(true);
                                }
                                else {
                                    RadioButton ex_F = (RadioButton)findViewById(R.id.edit_exercise_F);
                                    ex_F.setChecked(true);
                                }
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

    // Check that date is not a duplicate of another log. If so, show alert. If not, send put request.
    private void editLog() {
        final String dateText = ((EditText)findViewById(R.id.edit_date)).getText().toString();
        final int calories = Integer.valueOf(((EditText)findViewById(R.id.edit_calories)).getText().toString());
        final double weight = Double.valueOf(((EditText)findViewById(R.id.edit_weight)).getText().toString());
        final boolean exercised = ((RadioButton)findViewById(R.id.edit_exercise_T)).isChecked();

        try {
            mOkHttpClient = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs");
            reqUrl = reqUrl.newBuilder().addQueryParameter("date", dateText).build();

            // GET any post for the user at this date.
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
                        String r = response.body().string();

                        // If the row with the same date is not this entry, display alert.
                        if (!r.contains(rowID)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder dateAlert = new AlertDialog.Builder(EditLogActivity.this);
                                    dateAlert.setMessage("Whoops! You already made a log for that date.");
                                    dateAlert.setTitle("Duplicate date");
                                    dateAlert.setPositiveButton("OK", null);
                                    dateAlert.setCancelable(false);
                                    dateAlert.create().show();
                                }
                            });
                        }
                        // If there are no date conflicts, edit row.
                        else {
                            putLog(dateText, calories, weight, exercised);
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

    // Sends a PUT request to the API to edit the chosen row.
    private void putLog(final String dateText, final int calories, final double weight, final boolean exercised){
        try{
            mOkHttpClient = new OkHttpClient();
            String json = "{\"date\": \"" + dateText + "\", \"calories\": " + calories + ", \"weight\": " + weight + ", \"exercise\": " + exercised + "}";
            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs/" + rowID);

            // PUT to nutrition log API
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .put(body)
                    .build();

            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                // Once completed, return to main activity
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    finish();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Sends a PUT request to the API to edit the chosen row.
    private void deleteLog(){
        try{
            mOkHttpClient = new OkHttpClient();

            HttpUrl reqUrl = HttpUrl.parse("https://nutrition-log-176818.appspot.com/logs/" + rowID);

            // DELETE Request to nutrition log API
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .delete()
                    .build();

            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                // Once completed, return to main activity
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    finish();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Source: https://developer.android.com/guide/topics/ui/controls/pickers.html
    public void showDatePickerDialog(View v) {
        TextView dateText = (TextView)findViewById(R.id.edit_date);
        DialogFragment newFragment = new EditLogActivity.DatePickerFragment(dateText);
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
}
