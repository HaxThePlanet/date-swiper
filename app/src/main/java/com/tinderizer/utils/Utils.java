package com.tinderizer.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

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

    public static String getDeviceID(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
