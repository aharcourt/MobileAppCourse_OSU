package com.example.adeline.androidoauth;

// MainActivity.java
// Author: Adeline Harcourt based on skeleton code from Professor Justin Wolford in CS 496.
// Description: The main activity file for a simple app that utilizes Android OAuth and Google+
//          Domains API to allow the user to create a Google+ Post and View their last three
//          Google+ posts.


import android.support.v7.app.AppCompatActivity;
import android.app.PendingIntent;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuthorizationService = new AuthorizationService(this);

        // Set OnClickListener for "Submit Post" button to submit a text post to Google+.
        ((Button)findViewById(R.id.main_submitPost)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {
                        @Override
                        public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
                            if (e == null) {
                                mOkHttpClient = new OkHttpClient();
                                String postText = ((EditText)findViewById(R.id.main_postText)).getText().toString();
                                String json = "{'object': {'originalContent': '" + postText + "'},'access': {'domainRestricted': true}}";
                                HttpUrl reqUrl = HttpUrl.parse("https://www.googleapis.com/plusDomains/v1/people/me/activities");

                                // POST to Google+ Domains
                                RequestBody body = RequestBody.create(JSON, json);
                                Request request = new Request.Builder()
                                        .url(reqUrl)
                                        .addHeader("Authorization", "Bearer " + accessToken)
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
                                    }
                                });
                            }
                        }
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Set OnClickListener of "Refresh Posts" button to get last three posts from Google+
        ((Button)findViewById(R.id.main_refreshPosts)).setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                try {
                    mAuthState.performActionWithFreshTokens(mAuthorizationService, new AuthState.AuthStateAction() {

                        @Override
                        public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException e) {
                            if (e == null) {
                                mOkHttpClient = new OkHttpClient();
                                HttpUrl reqUrl = HttpUrl.parse("https://www.googleapis.com/plusDomains/v1/people/me/activities/user");
                                reqUrl = reqUrl.newBuilder().addQueryParameter("key", "AIzaSyAe5fKtX6yPVIYILbXR8a7eQdTqO5AeJ-8").build();

                                // GET from Google+ Domains
                                Request request = new Request.Builder()
                                        .url(reqUrl)
                                        .addHeader("Authorization", "Bearer " + accessToken)
                                        .build();

                                mOkHttpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        e.printStackTrace();
                                    }

                                    // Process response (Google+ Domains Posts)
                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        try {
                                            int postNum;                                    // Int to hold number of posts to display
                                            String r = response.body().string();            // Response string
                                            JSONObject j = new JSONObject(r);               // JSON response
                                            JSONArray items = j.getJSONArray("items");      // JSON array of response items
                                            List<Map<String, String>> posts = new ArrayList<Map<String, String>>();     // ArrayList of posts

                                            // Set number of posts to get equal to no more than three
                                            if (items.length() <= 3) {
                                                postNum = items.length();
                                            } else {
                                                postNum = 3;
                                            }

                                            // Get up to three posts and add to hashmap
                                            for (int i = 0; i < postNum; i++) {
                                                HashMap<String, String> m = new HashMap<String, String>();
                                                m.put("published", items.getJSONObject(i).getString("published"));
                                                m.put("title", items.getJSONObject(i).getString("title"));
                                                posts.add(m);
                                            }

                                            // Create adapter to add posts to listview
                                            final SimpleAdapter postAdapter = new SimpleAdapter (
                                                    MainActivity.this,
                                                    posts,
                                                    R.layout.activity_post_list_layout,
                                                    new String[]{"published", "title"},
                                                    new int[]{R.id.list_postDate, R.id.list_postText});
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((ListView) findViewById(R.id.main_PostList)).setAdapter(postAdapter);
                                                }
                                            });

                                        } catch (JSONException el) {
                                            el.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        mAuthState = this.getOrCreateAuthState();
        super.onStart();
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
        Uri redirect = new Uri.Builder().scheme("com.example.adeline.androidoauth").path("foobar").build();

        // Build request to send to API
        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint, null);
        AuthorizationRequest req = new AuthorizationRequest.Builder(config, "1090077299234-5fjld7e3q356gbd8h2ral35aq9hjjlth.apps.googleusercontent.com", ResponseTypeValues.CODE, redirect)
                .setScopes("https://www.googleapis.com/auth/plus.me", "https://www.googleapis.com/auth/plus.stream.write", "https://www.googleapis.com/auth/plus.stream.read")
                .build();

        // Create new pending intent to redirect return from request
        Intent authComplete = new Intent(this, AuthCompleteActivity.class);
        mAuthorizationService.performAuthorizationRequest(req, PendingIntent.getActivity(this, req.hashCode(), authComplete, 0));
    }
}






