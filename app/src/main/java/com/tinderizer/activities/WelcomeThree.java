package com.tinderizer.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WelcomeThree extends AppCompatActivity {
    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final int REQUEST_FINE_LOCATION_CODE = 0;

    @BindView(R.id.nextThreeButton)
    Button nextThreeButton;

    @BindView(R.id.drawer_layout)
    RelativeLayout mainLayout;

    private String deviceID;
    private EncryptedPreferences encryptedPreferences;

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
        setContentView(R.layout.activity_welcome_three);

        ButterKnife.bind(this);

        //hide login screen
//        Appsee.markViewAsSensitive(mainLayout);

        //logout
        nextThreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceID = Utils.getDeviceID(getApplicationContext());
                encryptedPreferences = new EncryptedPreferences.Builder(getApplicationContext()).withEncryptionPassword(deviceID).build();

                //not first run
                encryptedPreferences.edit()
                        .putBoolean(FIRST_RUN_KEY, false)
                        .apply();

                Intent myIntent = new Intent(WelcomeThree.this, MainActivity.class);
                startActivity(myIntent);

                finish();
            }
        });

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}

