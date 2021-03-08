package com.example.myapplication;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class firebase extends FirebaseInstanceIdService {

private static final String TAG = "MyFirebaseIIdService";

    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG,token);

        sendRegistrationToServer(token);


    }

    private void sendRegistrationToServer(String token){

    }
}
