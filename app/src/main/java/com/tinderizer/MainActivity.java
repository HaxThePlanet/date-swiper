package com.tinderizer;

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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.crashlytics.android.Crashlytics;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;

import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FINE_LOCATION_CODE = 0;
    private static String mGeolocationOrigin;
    private static GeolocationPermissions.Callback mGeolocationCallback;
    private static int numSwipes;
    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final String FAST_SWIPE_KEY = "FAST_SWIPE_KEY";
    private final String NOTIF_KEY = "NOTIF_KEY";
    WebView mWebview;
    Handler customHandler = new Handler();
    boolean go;
    int webviewHeight;
    boolean dashboardUp = false;
    CookieManager cookieManager = CookieManager.getInstance();
    HashSet likeHashMap = new HashSet();
    private RelativeLayout mContainer;
    private String url = "https://tinder.com/app/recs";
    private String target_url_prefix = "https://tinder.com/app/recs";
    private WebView mWebviewPop;
    private AlertDialog builder;
    private int minSpeed = 100;
    private int maxSpeed = 2000;
    //    private RewardedVideoAd mRewardedVideoAd;
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (go) {
                sendClick(mWebview);
            }

            customHandler.postDelayed(this, Utils.getRandomNumber(minSpeed, maxSpeed));
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

        //loading
        Intent myIntent = new Intent(this, LoadingActivity.class);
        this.startActivity(myIntent);

        setupSharedPrefs();

        Fabric.with(this, new Crashlytics());

        // Use an activity context to get the rewarded video instance.
//        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
//        mRewardedVideoAd.setRewardedVideoAdListener(this);


//        loadRewardedVideoAd();


        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_CODE);

        mWebview = (WebView) findViewById(R.id.tinder_webview);

        WebSettings webSettings = mWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptEnabled(true);
        mWebview.getSettings().setSavePassword(true);
        mWebview.getSettings().setSaveFormData(true);
        mWebview.setWebViewClient(new UriWebViewClient());
        mWebview.setWebChromeClient(new GeoWebChromeClient());
        mWebview.getSettings().setSavePassword(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(mWebview, true);
        }

        cookieManager.setAcceptCookie(true);

        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i("chadtest", url);

                if (url.contains("/recs/core")) {
                    if (dashboardUp == false) {
                        dashboardUp = true;

                        //close loading screen
                        EventBus.getDefault().post(new MessageEvents.CloseLoading());

                        //show dashboard
                        Intent myIntent = new Intent(MainActivity.this, DashboardActivity.class);
                        startActivity(myIntent);
                    }
                }

                if (url.contains("/like/")) {
                    if (!likeHashMap.contains(url)) {
                        likeHashMap.add(url);
                    } else {
                        //does
                        numSwipes = numSwipes + 1;

                        Log.i("chadlike", url);
                    }
                }

                if (url.contains("buckets")) {
                    //close loading screen
                    EventBus.getDefault().post(new MessageEvents.CloseLoading());
                }
            }

//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                String host = Uri.parse(url).getHost();
//                //Log.d("shouldOverrideUrlLoading", url);
//                if (host.equals(target_url_prefix))
//                {
//                    // This is my web site, so do not override; let my WebView load
//                    // the page
////                    if(mWebviewPop!=null)
////                    {
////                        mWebviewPop.setVisibility(View.GONE);
////                        mContainer.removeView(mWebviewPop);
////                        mWebviewPop=null;
////                    }
//                    return false;
//                }
//
//                if(host.equals("m.facebook.com") || host.equals("www.facebook.com"))
//                {
//                    if(mWebviewPop!=null) {
//                        mWebviewPop.loadUrl("https://tinder.com/app/recs");
//                    }
//                    return false;
//                }
//                // Otherwise, the link is not for a page on my site, so launch
//                // another Activity that handles URLs
////                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
////                startActivity(intent);
//
//
//
//                return false;
//            }
        });

        mWebview.getSettings().setAllowContentAccess(true);
        mWebview.getSettings().setDomStorageEnabled(true);
        mWebview.getSettings().setAllowFileAccess(true);
        mWebview.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebview.getSettings().setAppCacheEnabled(true);
        mWebview.getSettings().setDatabaseEnabled(true);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.getSettings().setGeolocationEnabled(true);
        mWebview.setWebChromeClient(new GeoWebChromeClient());

        mWebview.loadUrl("https://www.tinder.com/app/recs");

        webviewHeight = getWindowManager().getDefaultDisplay().getHeight() / 2;

        android.os.Handler customHandler = new android.os.Handler();
        customHandler.postDelayed(updateTimerThread, 0);
    }

    //setup shared prefs initially
    private void setupSharedPrefs() {
        String deviceID = Utils.getDeviceID(this);
        EncryptedPreferences encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();
        boolean isFirstRun = encryptedPreferences.getBoolean(FIRST_RUN_KEY, true);

        //first run?
        if (isFirstRun) {
            //set default settings
            encryptedPreferences.edit()
                    .putBoolean(FIRST_RUN_KEY, false)
                    .putBoolean(NOTIF_KEY, true)
                    .putBoolean(FAST_SWIPE_KEY, false)
                    .apply();
        }
    }

    private void loadRewardedVideoAd() {
//        mRewardedVideoAd.loadAd("ca-app-pub-5336818452987335/9055464537",
//                new AdRequest.Builder().build());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    private void sendClick(WebView wv) {
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
        if (mWebview.canGoBack()) {
        } else {
            mWebview.goBack();
            super.onBackPressed();
        }
    }

    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (host.equals(target_url_prefix)) {
                // This is my web site, so do not override; let my WebView load
                // the page
//                if(mWebviewPop!=null)
//                {
//                    mWebviewPop.setVisibility(View.GONE);
//                    mContainer.removeView(mWebviewPop);
//                    mWebviewPop=null;
//                }
                return false;
            }

            if (host.equals("m.facebook.com") || host.equals("www.facebook.com")) {
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch
            // another Activity that handles URLs
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//            startActivity(intent);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
            Log.d("onReceivedSslError", "onReceivedSslError");
            //super.onReceivedSslError(view, handler, error);
        }

    }

    private class UriWebViewClient extends WebViewClient {
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            String host = Uri.parse(url).getHost();
//
//            //looking for fb button load auth page press
//            if (host.contains("oauth")) {
//                //redir to normal fb auth page
//
//                view.loadUrl("https://www.tinder.com/app/recs");
//                return true;
//            }
//
//            return false;
//        }
//
//        @Override
//        public void onReceivedSslError(WebView view, SslErrorHandler handler,  SslError error) {
//            Log.d("onReceivedSslError", "onReceivedSslError");
//        }
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
//            builder.setButton("Close", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int id) {
//                    mWebviewPop.destroy();
//                    dialog.dismiss();
//                }
//            });
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

