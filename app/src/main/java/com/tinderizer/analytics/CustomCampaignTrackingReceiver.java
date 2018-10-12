package com.tinderizer.analytics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.analytics.CampaignTrackingReceiver;

public class CustomCampaignTrackingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // When you're done, pass the intent to the Google Analytics receiver.
        new CampaignTrackingReceiver().onReceive(context, intent);

        Log.v("ReferralReceiver", " " + intent.getAction());
        Log.v("ReferralReceiver", " " + intent.getDataString());
        Log.v("ReferralReceiver", " " + intent.toString());
        Log.v("ReferralReceiver", " " + intent.getStringExtra("referrer"));

        //call to other referrers

    }
}
