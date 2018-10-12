package com.tinderizer.utils;

import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import java.util.Random;

public class Utils {
    private static final String RECS_URL_BASE64 = "aHR0cHM6Ly93d3cudGluZGVyLmNvbS9hcHAvcmVjcw==";
    private static final String COM_PACKAGE_BASE64 = "Y29tLnRpbmRlcg==";
    private static final String ENCODING_TYPE = "UTF-8";
    private static boolean isPurch;
    private static boolean outLikes;
    private static long lastSuccessRecTime;
    private static long FORCE_REFRESH_WEBVIEW_MILLISECONDS = 30000;

    public static int getRandomNumber(int min, int max) {
        return (new Random()).nextInt((max - min) + 1) + min;
    }

    public static void killLoginSession(WebView wv, Context ctx) {
        try {
            //clear cache
            wv.clearCache(true);
            wv.clearHistory();
        } catch (Exception ex) {
        }

        try {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } catch (Exception ex) {
        }

        try {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(ctx);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        } catch (Exception ex) {
        }
    }

    public static String getRecsUrl() {
        try {
            byte[] tmp2 = Base64.decode(RECS_URL_BASE64, 0);
            return new String(tmp2, ENCODING_TYPE);
        } catch (Exception ex) {
        }

        return "";
    }

    public static String getComPackage() {
        try {
            byte[] tmp2 = Base64.decode(COM_PACKAGE_BASE64, 0);
            return new String(tmp2, ENCODING_TYPE);
        } catch (Exception ex) {
        }

        return "";
    }

    public static boolean isOutOfLikes() {
        return outLikes;
    }

    public static void setOutOfLikes(boolean likes) {
        outLikes = likes;
    }

    public static boolean shouldRefreshWebview() {
        if (lastSuccessRecTime == 0) return false;

        if (SystemClock.uptimeMillis() - lastSuccessRecTime >= FORCE_REFRESH_WEBVIEW_MILLISECONDS) {
            //refresh
            return true;
        }

        return false;
    }

    public static void setSwipeTime(long time) {
        lastSuccessRecTime = time;
    }

    public static boolean isPurchased() {
        return isPurch;
    }

    public static void setPurchased(boolean purch) {
        isPurch = purch;
    }

    public static String getDeviceID(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void sendWebviewTouchSwipe(WebView wv, float webviewHeight, float webviewWidth) {
        //send swipe
        sendSwipeGesture(wv, webviewHeight, webviewWidth);

        //send keep swiping
        sendKeepSwipingGesture(wv, webviewHeight, webviewWidth);
    }

    //keep swiping button is wv h - 65%
    private static void sendKeepSwipingGesture(WebView wv, float webviewHeight, float webviewWidth) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 10;

        //wv.dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, webviewWidth / 2, webviewHeight * 0.59f, 0));
        //wv.dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, webviewWidth / 2, webviewHeight * 0.59f, 0));

        wv.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE));
        wv.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE));

    }

    //swipe event
    //down -> move -> up
    private static void sendSwipeGesture(WebView wv, float webviewHeight, float webviewWidth) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 10;

        wv.dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, webviewWidth / 2, webviewHeight / 2, 0));
        wv.dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, webviewWidth / 2 + webviewWidth, webviewHeight / 2, 0));
        wv.dispatchTouchEvent(MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, webviewWidth / 2, webviewHeight / 2, 0));
    }
}
