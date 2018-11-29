package com.tswiper.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tswiper.R;
import com.tswiper.events.MessageEvents;
import com.tswiper.utils.DateTimeUtils;
import com.tswiper.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.toptas.fancyshowcase.FancyShowCaseView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class DashboardActivity extends AppCompatActivity {
    private static final int freeLikesCount = 100;
    private static boolean showingOutLikesAlert;
    @BindView(R.id.swipeCount)
    TextView swipeCount;
    @BindView(R.id.adView)
    AdView adView;
    @BindView(R.id.playPauseImg)
    ImageView playPauseImg;
    @BindView(R.id.bottom_layout)
    RelativeLayout bottomLayout;
    @BindView(R.id.swipes_layout)
    RelativeLayout swipesLayout;
    boolean isPlay = false;
    @BindView(R.id.settingsGear)
    ImageView settingsGear;
    @BindView(R.id.swipeProgress)
    ProgressBar swipeProgress;
    SharedPreferences preferences;

    @BindView(R.id.titleText)
    TextView titleText;

    private AdView mAdView;
    private int todaysLikes;
    private String deviceID;
    private EncryptedPreferences encryptedPreferences;

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

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

    private void setupFonts() {
//        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
//                .setDefaultFontPath("fonts/fine.otf")
//                .setFontAttrId(R.attr.fontPath)
//                .build()
//        );
    }

    @Override
    public void onPause() {
        adView.getVideoController().pause();
        adView.pause();

        EventBus.getDefault().post(new MessageEvents.StopWebview());
        super.onPause();
    }

    @Override
    public void onResume() {
        adView.resume();
        adView.getVideoController().play();

        EventBus.getDefault().post(new MessageEvents.StartWebview());

        setTitle();

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ButterKnife.bind(this);

        setupFonts();
        setTitle();

        EventBus.getDefault().register(this);

        deviceID = Utils.getDeviceID(this);
        encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();

        //get todays date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy-MM-dd");
        String dateTime = mdformat.format(calendar.getTime());

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        todaysLikes = preferences.getInt(dateTime, 0);

        swipeCount.setText("Free Swipes Remaining " + String.valueOf(freeLikesCount - todaysLikes) + " (resets in " + DateTimeUtils.getTimeUntilMidnight() + ")");
        swipeProgress.setProgress(freeLikesCount - todaysLikes);

        if (!Utils.isPurchased()) {
            MobileAds.initialize(this, getString(R.string.admob_ad_unit));

            mAdView = findViewById(R.id.adView);
            // AdRequest adRequest = new AdRequest.Builder().addTestDevice("4E09B12D5BD9AFF27E5EB2C6D3EFB27D").build();
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    mAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    // Code to be executed when an ad request fails.
                    mAdView.setVisibility(View.GONE);
                }

                @Override
                public void onAdOpened() {
                    // Code to be executed when the ad is displayed.
                }

                @Override
                public void onAdLeftApplication() {
                    // Code to be executed when the user has left the app.
                }

                @Override
                public void onAdClosed() {
                    // Code to be executed when when the interstitial ad is closed.
                }
            });
        }

        settingsGear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(DashboardActivity.this, SettingsActivity.class);
                startActivity(myIntent);
            }
        });

        playPauseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlay) {
                    if (!Utils.isOutOfLikes()) {
                        isPlay = false;
                        playPauseImg.setBackground(getDrawable(R.drawable.play));
                        EventBus.getDefault().post(new MessageEvents.ButtonPauseEvent());
                    } else {
                        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(DashboardActivity.this);
                        builder
                                .setTitle("You are out of swipes for the day")
                                .setMessage("You have used up your " + String.valueOf(freeLikesCount) + " swipes for the day. Would you like unlimited swipes?")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        showingOutLikesAlert = false;

                                        dialog.dismiss();

                                        //show settings
                                        Intent myIntent = new Intent(DashboardActivity.this, SettingsActivity.class);
                                        myIntent.putExtra("open_upgrade", "open_upgrade");
                                        startActivity(myIntent);
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        showingOutLikesAlert = false;
                                        dialog.dismiss();
                                    }
                                })

                                .show();
                    }
                } else {
                    if (!Utils.isOutOfLikes()) {
                        isPlay = true;
                        playPauseImg.setBackground(getDrawable(R.drawable.pause));
                        EventBus.getDefault().post(new MessageEvents.ButtonPlayEvent());
                    } else {
                        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(DashboardActivity.this);
                        builder
                                .setTitle("You are out of swipes for the day")
                                .setMessage("You have used up your " + String.valueOf(freeLikesCount) + " swipes for the day. Would you like unlimited swipes?")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        showingOutLikesAlert = false;

                                        dialog.dismiss();

                                        //show settings
                                        Intent myIntent = new Intent(DashboardActivity.this, SettingsActivity.class);
                                        myIntent.putExtra("open_upgrade", "open_upgrade");
                                        startActivity(myIntent);
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        showingOutLikesAlert = false;
                                        dialog.dismiss();
                                    }
                                })

                                .show();
                    }
                }
            }
        });

        if (Utils.isPurchased()) {
            swipeProgress.setVisibility(View.INVISIBLE);
            adView.setVisibility(View.INVISIBLE);
            swipeCount.setText("Total Swipes " + String.valueOf(getTotalSwipes()));
        } else {
            swipeProgress.setVisibility(View.VISIBLE);
            adView.setVisibility(View.VISIBLE);
        }
    }

    private void setTitle() {
        if (Utils.isPurchased()) {
            titleText.setText(getString(R.string.app_name_pro));
        } else {
            titleText.setText(getString(R.string.app_name));
        }
    }

    private int getTotalSwipes() {
        return preferences.getInt("total_swipes", 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.SwipeEvent event) {
        if (Utils.isPurchased()) {
            int totalSwipes = getTotalSwipes();

            swipeCount.setText("Total Swipes " + String.valueOf(totalSwipes));

            return;
        }

        swipeCount.setText("Free Swipes Remaining " + String.valueOf(freeLikesCount - todaysLikes) + " (resets in " + DateTimeUtils.getTimeUntilMidnight() + ")");
        swipeProgress.setProgress(freeLikesCount - todaysLikes);

        //get todays date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy-MM-dd");

        //read date key + count
        todaysLikes = preferences.getInt(mdformat.format(calendar.getTime()), 0);
        todaysLikes += 1;

        //write todays likes
        String dateTime = mdformat.format(calendar.getTime());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(dateTime, todaysLikes);
        editor.apply();
        editor.commit();

        if (todaysLikes >= freeLikesCount) {
            //set text/progress
            swipeCount.setText("Free Swipes Remaining " + String.valueOf(0) + " (resets in " + DateTimeUtils.getTimeUntilMidnight() + ")");
            swipeProgress.setProgress(0);

            //save
            editor = preferences.edit();
            editor.putInt(dateTime, freeLikesCount);
            editor.apply();
            editor.commit();

            //set out of likes
            Utils.setOutOfLikes(true);

            isPlay = false;
            playPauseImg.setBackground(getDrawable(R.drawable.play));

            //pause swiper
            EventBus.getDefault().post(new MessageEvents.ButtonPauseEvent());

            if (!showingOutLikesAlert) {
                showingOutLikesAlert = true;

                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(DashboardActivity.this);
                builder
                        .setTitle("You are out of swipes for the day")
                        .setMessage("You have used up your " + String.valueOf(freeLikesCount) + " swipes for the day. Would you like unlimited swipes?")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showingOutLikesAlert = false;

                                dialog.dismiss();

                                //show settings
                                Intent myIntent = new Intent(DashboardActivity.this, SettingsActivity.class);
                                myIntent.putExtra("open_upgrade", "open_upgrade");
                                startActivity(myIntent);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showingOutLikesAlert = false;
                                dialog.dismiss();
                            }
                        })

                        .show();
            }
        } else {
            Utils.setOutOfLikes(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.AppNotPurchased event) {
        Utils.setPurchased(false);
        swipeProgress.setVisibility(View.VISIBLE);
        adView.setVisibility(View.VISIBLE);
        swipeCount.setText("Free Swipes Remaining " + String.valueOf(freeLikesCount - todaysLikes) + " (resets in " + DateTimeUtils.getTimeUntilMidnight() + ")");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.AppPurchased event) {
        Utils.setPurchased(true);
        swipeProgress.setVisibility(View.INVISIBLE);
        adView.setVisibility(View.INVISIBLE);
        swipeCount.setText("Total Swipes " + String.valueOf(getTotalSwipes()));
    }
}

