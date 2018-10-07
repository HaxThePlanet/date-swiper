package com.tinderizer.utils;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Base64;
import android.view.MotionEvent;
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

    public static String getRecsUrl() {
        String ps2 = "aHR0cHM6Ly93d3cudGluZGVyLmNvbS9hcHAvcmVjcw==";

        try {
            byte[] tmp2 = Base64.decode(ps2,0);
            return new String(tmp2, "UTF-8");
        } catch (Exception ex) {
        }

        return "";
    }

    public static String getComPackage() {
        String ps2 = "Y29tLnRpbmRlcg==";

        try {
            byte[] tmp2 = Base64.decode(ps2,0);
            return new String(tmp2, "UTF-8");
        } catch (Exception ex) {
        }

        return "";
    }

    public static String getDeviceID(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void sendClick(WebView wv, float webviewHeight) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 10;
        float x = 700f;
        float y = webviewHeight;
        int metaState = 0;

        //get ride of match screen
        MotionEvent down = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );

        wv.dispatchTouchEvent(down);

        MotionEvent up = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,

                metaState
        );

        //swipe gesture
        MotionEvent down2 = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );

        wv.dispatchTouchEvent(down2);

        MotionEvent move = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                1400,
                y,
                metaState
        );

        wv.dispatchTouchEvent(move);

        MotionEvent up2 = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                1600,
                y,

                metaState
        );

        wv.dispatchTouchEvent(up2);
    }
}
