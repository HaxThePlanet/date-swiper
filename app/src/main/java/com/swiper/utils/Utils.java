package com.swiper.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.swiper.LoadingActivity;

import java.util.Random;

public class Utils {
    public static int getRandomNumber(int min, int max) {
        return (new Random()).nextInt((max - min) + 1) + min;
    }

    @SuppressWarnings("deprecation")
    public static void killLoginSession(WebView wv, Context ctx) {
        //clear cache
        wv.clearCache(true);
        wv.clearHistory();

        //kill cookies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
//        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(ctx);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    private void showLoading(Context ctx) {
        Intent myIntent = new Intent(ctx, LoadingActivity.class);
        ctx.startActivity(myIntent);
    }

    private void hideLoading() {

    }
}
