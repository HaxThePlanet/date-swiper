package com.tinderizer.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {
    private static String mGeolocationOrigin;
    private static GeolocationPermissions.Callback mGeolocationCallback;
    private static int numSwipes;

    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final String FAST_SWIPE_KEY = "FAST_SWIPE_KEY";
    private final String NOTIF_KEY = "NOTIF_KEY";
    private final String TOTAL_SWIPES_KEY = "SWIPES_KEY";
    private final int REQUEST_FINE_LOCATION_CODE = 0;

    @BindView(R.id.webview)
    WebView webview;

    private Handler customHandler = new Handler();
    private boolean go;
    private int webviewHeight;
    private boolean dashboardUp = false;

    private CookieManager cookieManager = CookieManager.getInstance();
    private HashSet likeHashMap = new HashSet();

    private WebView mWebviewPop;
    private AlertDialog builder;

    private int slowMinSpeed = 1000;
    private int slowMaxSpeed = 3000;

    private int fastMinSpeed = 250;
    private int fastMaxSpeed = 1500;

    private String deviceID;
    private EncryptedPreferences encryptedPreferences;

    //    private RewardedVideoAd mRewardedVideoAd;
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (go) {
                Utils.sendClick(webview, webviewHeight);
            }
            if (isFastSwipeEnabled()) {
                customHandler.postDelayed(this, Utils.getRandomNumber(fastMinSpeed, fastMaxSpeed));
            } else {
                customHandler.postDelayed(this, Utils.getRandomNumber(slowMinSpeed, slowMaxSpeed));
            }

            EventBus.getDefault().post(new MessageEvents.SwipeEvent(numSwipes));
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.PlayEvent event) {
        go = true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.PauseEvent event) {
        go = false;
    }

    @Override
    public void onPause() {
        super.onPause();
//        mRewardedVideoAd.pause(this);
    }

    @Override
    public void onDestroy() {
//        mRewardedVideoAd.destroy(this);
        super.onDestroy();
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

    @Override
    public void onResume() {
//        mRewardedVideoAd.resume(this);
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceID = Utils.getDeviceID(this);
        encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();

        ButterKnife.bind(this);

        // Use an activity context to get the rewarded video instance.
//        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
//        mRewardedVideoAd.setRewardedVideoAdListener(this);
//        loadRewardedVideoAd();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setupWebview();

        android.os.Handler customHandler = new android.os.Handler();
        customHandler.postDelayed(updateTimerThread, 0);
    }

    private void setupWebview() {
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptEnabled(true);
        webview.getSettings().setSavePassword(true);
        webview.getSettings().setSaveFormData(true);
        webview.setWebViewClient(new UriWebViewClient());
        webview.setWebChromeClient(new GeoWebChromeClient());
        webview.getSettings().setSavePassword(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webview, true);
        }

        cookieManager.setAcceptCookie(true);

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i("chadtest", url);

                EventBus.getDefault().post(new MessageEvents.CloseLoading());

                //we're at like screen
                if (url.contains("/recs/core")) {
                    //close loading screen
                    EventBus.getDefault().post(new MessageEvents.CloseLoading());

                    if (dashboardUp == false) {
                        dashboardUp = true;

                        //show dashboard
                        Intent myIntent = new Intent(MainActivity.this, DashboardActivity.class);
                        startActivity(myIntent);
                    }
                }

                //like happened
                if (url.contains("/like/")) {
                    if (!likeHashMap.contains(url)) {
                        likeHashMap.add(url);
                    } else {
                        //does
                        numSwipes = numSwipes + 1;

                        Log.i("chadlike", url);
                    }
                }

                //login page
                if (url.contains("buckets")) {
                    //close loading screen
                    EventBus.getDefault().post(new MessageEvents.CloseLoading());
                }
            }
        });

        webview.getSettings().setAllowContentAccess(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setAllowFileAccessFromFileURLs(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webview.getSettings().setAppCacheEnabled(true);
        webview.getSettings().setDatabaseEnabled(true);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setGeolocationEnabled(true);
        webview.setWebChromeClient(new GeoWebChromeClient());

        webview.loadUrl(Utils.getRecsUrl());
        webviewHeight = getWindowManager().getDefaultDisplay().getHeight() / 2;

        //not first run
        encryptedPreferences.edit()
                .putBoolean(FIRST_RUN_KEY, false)
                .apply();

    }

    private int getSwipes() {
        return encryptedPreferences.getInt(TOTAL_SWIPES_KEY, 0);
    }

    private void putSwipe(int swipeCount) {
        //set default settings
        encryptedPreferences.edit()
                .putInt(TOTAL_SWIPES_KEY, swipeCount)
                .apply();
    }

    private boolean isFastSwipeEnabled() {
        return encryptedPreferences.getBoolean(FAST_SWIPE_KEY, false);
    }

    private void loadRewardedVideoAd() {
        //mRewardedVideoAd.loadAd("ca-app-pub-5336818452987335/9055464537", new AdRequest.Builder().build());
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
                boolean allow = false;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user has allowed this permission
                    allow = true;
                }
                if (mGeolocationCallback != null) {
                    // call back to web chrome client
                    mGeolocationCallback.invoke(mGeolocationOrigin, allow, true);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // Pop the browser back stack or exit the activity
        if (webview.canGoBack()) {
        } else {
            webview.goBack();
            super.onBackPressed();
        }
    }

    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (host.equals(Utils.getRecsUrl())) {
                return false;
            }

            if (host.equals("m.facebook.com") || host.equals("www.facebook.com")) {
                return false;
            }

            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
        }
    }

    private class UriWebViewClient extends WebViewClient {
    }

    /**
     * WebChromeClient subclass handles UI-related calls
     * Note: think chrome as in decoration, not the Chrome browser
     */
    public class GeoWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            mWebviewPop = new WebView(getApplicationContext());
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebViewClient(new UriWebViewClient());
            mWebviewPop.setWebChromeClient(new GeoWebChromeClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setSavePassword(true);
            mWebviewPop.getSettings().setSaveFormData(true);
            builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).create();
            builder.setTitle("");
            builder.setView(mWebviewPop);
            builder.show();
            builder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(mWebviewPop, true);
            }
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            try {
                mWebviewPop.destroy();
            } catch (Exception e) {
            }
            try {
                builder.dismiss();
            } catch (Exception e) {
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // Geolocation permissions coming from this app's Manifest will only be valid for devices with
            // API_VERSION < 23. On API 23 and above, we must check for permissions, and possibly
            // ask for them.
            String perm = Manifest.permission.ACCESS_FINE_LOCATION;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    ContextCompat.checkSelfPermission(MainActivity.this, perm) == PackageManager.PERMISSION_GRANTED) {
                // we're on SDK < 23 OR user has already granted permission
                callback.invoke(origin, true, false);
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, perm)) {
                    // ask the user for permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{perm}, REQUEST_FINE_LOCATION_CODE);

                    // we will use these when user responds
                    mGeolocationOrigin = origin;
                    mGeolocationCallback = callback;
                }
            }
        }
    }
}

