package com.example.adeline.hybridnutritionlog;

// AuthCompleteActivity.java
// Author: Adeline Harcourt based on skeleton code from Professor Justin Wolford in CS 496.
// Description: The AuthCompleteActivityv file for a simple app that utilizes a cloud-based nutritional
// log API in order to allow users to save nutritional logs. This activity handles the response back from
// Google's authentication server.

import android.support.v7.app.AppCompatActivity;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Bundle;
import android.content.SharedPreferences;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

// Activity that handles the response back from Google's authentication server
public class AuthCompleteActivity extends AppCompatActivity {
    private AuthorizationService mAuthorizationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_complete);

        mAuthorizationService = new AuthorizationService(this);
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(getIntent());
        AuthorizationException ex = AuthorizationException.fromIntent(getIntent());


        // If response came back, create an AuthState
        if(resp != null){
            final AuthState authState = new AuthState(resp, ex);
            mAuthorizationService.performTokenRequest(resp.createTokenExchangeRequest(),
                    new AuthorizationService.TokenResponseCallback() {
                        // Store token in Shared Preferences
                        @Override
                        public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException e) {
                            authState.update(tokenResponse,e);
                            SharedPreferences authPreferences = getSharedPreferences("auth", MODE_PRIVATE);
                            authPreferences.edit().putString("stateJson", authState.jsonSerializeString()).apply();
                            finish();
                        }
                    });
        }
    }
}

