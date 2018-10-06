package com.swiper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SettingsActivity extends AppCompatActivity {
    @BindView(R.id.changeLocation)
    ImageButton changeLocation;

    @BindView(R.id.logoutButton)
    Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        //change location
        changeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder
                        .setTitle("Change Tinder location?")
                        .setMessage("Tinder Swiper will exit, you will need to change your location in the Tinder app")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.tinder");
                                if (launchIntent != null) {
                                    startActivity(launchIntent);
                                } else {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                                    alert.setTitle("");
                                    alert.setMessage("Please install the Tinder app");
                                    alert.setPositiveButton("OK",null);
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
                        .setMessage("Logout of Tinder Swiper? Tinder Swiper will close, you will need to restart the app")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                logoutTinder();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void logoutTinder() {
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
        ((ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();

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

