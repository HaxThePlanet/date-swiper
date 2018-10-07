package com.tinderizer.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LoadingActivity extends AppCompatActivity {
    private String deviceID;
    private EncryptedPreferences encryptedPreferences;

    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final String FAST_SWIPE_KEY = "FAST_SWIPE_KEY";
    private final String NOTIF_KEY = "NOTIF_KEY";
    private final String TOTAL_SWIPES_KEY = "SWIPES_KEY";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.CloseLoading event) {
        this.finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        //first run?
        setupSharedPrefs();
    }

    //setup shared prefs initially
    private void setupSharedPrefs() {
        deviceID = Utils.getDeviceID(this);
        encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();
        boolean isFirstRun = encryptedPreferences.getBoolean(FIRST_RUN_KEY, true);

        //first run?
        if (isFirstRun) {
            //set default settings
            encryptedPreferences.edit()
                    .putBoolean(NOTIF_KEY, true)
                    .putBoolean(FAST_SWIPE_KEY, false)
                    .apply();

            //first run
            Intent myIntent = new Intent(this, WelcomeOne.class);
            this.startActivity(myIntent);

            finish();
        } else {
            //NOT first run
            Intent myIntent = new Intent(this, MainActivity.class);
            this.startActivity(myIntent);

            this.finish();
        }
    }


}

