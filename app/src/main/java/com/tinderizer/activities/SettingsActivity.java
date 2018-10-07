package com.tinderizer.activities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SettingsActivity extends AppCompatActivity {
    private final String FAST_SWIPE_KEY = "FAST_SWIPE_KEY";
    private final String NOTIF_KEY = "NOTIF_KEY";

    @BindView(R.id.changeLocation)
    ImageButton changeLocation;

    @BindView(R.id.logoutButton)
    Button logoutButton;

    @BindView(R.id.notifSwitch)
    Switch notifSwitch;

    @BindView(R.id.fastSwipeSwitch)
    Switch fastSwipeSwitch;

    String deviceID;
    EncryptedPreferences encryptedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        deviceID = Utils.getDeviceID(this);
        encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();

        setupSwitches();

        //change location
        changeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder
                        .setTitle("Change location?")
                        .setMessage("Date Swiper will exit, you will need to change your location in the dating app")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(Utils.getComPackage());
                                if (launchIntent != null) {
                                    startActivity(launchIntent);

                                    //exit
                                    System.exit(1);
                                } else {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                                    alert.setTitle("");
                                    alert.setMessage("Dating app not installed");
                                    alert.setPositiveButton("OK", null);
                                    alert.show();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        //logout
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder
                        .setTitle("Are you sure?")
                        .setMessage("Logout of Date Swiper? Date Swiper will close, you will need to restart the app")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                logoutDatingApp();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        notifSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                encryptedPreferences.edit()
                        .putBoolean(NOTIF_KEY, isChecked)
                        .apply();
            }
        });

        fastSwipeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                encryptedPreferences.edit()
                        .putBoolean(FAST_SWIPE_KEY, isChecked)
                        .apply();
            }
        });
    }

    private void setupSwitches() {
        notifSwitch.setChecked(encryptedPreferences.getBoolean(NOTIF_KEY, true));
        fastSwipeSwitch.setChecked(encryptedPreferences.getBoolean(FAST_SWIPE_KEY, false));
    }

    private void logoutDatingApp() {
        //schedule restart
//        try {
//            Intent mStartActivity = new Intent(SettingsActivity.this, MainActivity.class);
//            int mPendingIntentId = 123456;
//            PendingIntent mPendingIntent = PendingIntent.getActivity(SettingsActivity.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
//            AlarmManager mgr = (AlarmManager) SettingsActivity.this.getSystemService(Context.ALARM_SERVICE);
//            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, mPendingIntent);
//        } catch (Exception ex) {
//        }

        //blow cache
        ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();

        //exit
        System.exit(1);
    }

    @Override
    public void onStop() {
        super.onStop();
//        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
//        if (!EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().register(this);
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}

