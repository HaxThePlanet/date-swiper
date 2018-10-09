package com.tinderizer.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import test.jinesh.captchaimageviewlib.CaptchaImageView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

public class WelcomeThree extends AppCompatActivity {
    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final int REQUEST_FINE_LOCATION_CODE = 0;

    @BindView(R.id.nextThreeButton)
    Button nextThreeButton;

    @BindView(R.id.captchaImage)
    CaptchaImageView captchaImage;

    @BindView(R.id.captchaAnswerTextview)
    TextView captchaAnswerTextview;

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

        //logout
        nextThreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (captchaImage.getCaptchaCode().equalsIgnoreCase(captchaAnswerTextview.getText().toString())) {
                    deviceID = Utils.getDeviceID(getApplicationContext());
                    encryptedPreferences = new EncryptedPreferences.Builder(getApplicationContext()).withEncryptionPassword(deviceID).build();

                    //not first run anymore
                    encryptedPreferences.edit()
                            .putBoolean(FIRST_RUN_KEY, false)
                            .apply();

                    // run main
                    Intent myIntent = new Intent(WelcomeThree.this, MainActivity.class);
                    startActivity(myIntent);

                    finish();
                } else {
                    //wrong answer
                    android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeThree.this);
                    builder
                            .setTitle("Incorrect Answer")
                            .setMessage("Your Captcha answer is incorrect, please try again")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    captchaAnswerTextview.setText("");
                                    captchaImage.regenerate();
                                }
                            })
                            .show();
                }

            }
        });

        captchaImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captchaImage.regenerate();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_FINE_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    deviceID = Utils.getDeviceID(this);
                    encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();

                    //not first run
                    encryptedPreferences.edit()
                            .putBoolean(FIRST_RUN_KEY, false)
                            .apply();

                    Intent myIntent = new Intent(WelcomeThree.this, MainActivity.class);
                    startActivity(myIntent);

                    finish();
                }
        }
    }
}

