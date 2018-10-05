package com.swiper;

import android.Manifest;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.swiper.messaging.MessageEvents;
import com.swiper.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FINE_LOCATION_CODE = 0;
    private static String mGeolocationOrigin;
    private static GeolocationPermissions.Callback mGeolocationCallback;
    private static int numSwipes;
    WebView mWebView;
    Handler customHandler = new Handler();
    boolean go;
    int webviewHeight;
    boolean dashboardUp = false;
    private WebView mWebviewPop;
    private RelativeLayout mContainer;

    private String url = "https://tinder.com/app/recs";
    private String target_url_prefix = "https://tinder.com/app/recs";

    //Cookie manager for the webview
    CookieManager cookieManager = CookieManager.getInstance();

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (go) {
                sendClick(mWebView);
                numSwipes++;
            }

            Log.i("chad", String.valueOf(numSwipes));

            customHandler.postDelayed(this, Utils.getRandomNumber(250, 3000));

            EventBus.getDefault().post(new MessageEvents.SwipeEvent(numSwipes));
        }
    };

    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.SwipeEvent event) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.PlayEvent event) {
        go = true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.PauseEvent event) {
        go = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        EventBus.getDefault().register(this);

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_CODE);

        mWebView = (WebView) findViewById(R.id.webView1);
        // Brower niceties -- pinch / zoom, follow links in place
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        //Get outer container
        mContainer = (RelativeLayout) findViewById(R.id.webview_frame);

        cookieManager.setAcceptCookie(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i("chadtest", url);

                if (url.contains("/batch/event")) {
                    if (dashboardUp == false) {
                        dashboardUp = true;
//                        Intent myIntent = new Intent(MainActivity.this, DashboardActivity.class);
//                        startActivity(myIntent);
                    }
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String host = Uri.parse(url).getHost();
                //Log.d("shouldOverrideUrlLoading", url);
                if (host.equals(target_url_prefix))
                {
                    // This is my web site, so do not override; let my WebView load
                    // the page
//                    if(mWebviewPop!=null)
//                    {
//                        mWebviewPop.setVisibility(View.GONE);
//                        mContainer.removeView(mWebviewPop);
//                        mWebviewPop=null;
//                    }
                    return false;
                }

                if(host.equals("m.facebook.com") || host.equals("www.facebook.com"))
                {
                    if(mWebviewPop!=null) {
                        mWebviewPop.loadUrl("https://tinder.com/app/recs");
                    }
                    return false;
                }
                // Otherwise, the link is not for a page on my site, so launch
                // another Activity that handles URLs
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                startActivity(intent);



                return false;
            }
        });

        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);

        // Below required for geolocation
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.setWebChromeClient(new GeoWebChromeClient());
        // Load google.com
        mWebView.loadUrl("https://tinder.com/app/recs");

        webviewHeight =  getWindowManager().getDefaultDisplay().getHeight() /2;

        android.os.Handler customHandler = new android.os.Handler();
        customHandler.postDelayed(updateTimerThread, 0);
    }

    private void sendClick(WebView wv) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 10;
        float x = 700f;
        float y = webviewHeight;
        int metaState = 0;

        MotionEvent down = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );

        wv.dispatchTouchEvent(down);

        MotionEvent move = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                1400,
                y,
                metaState
        );

        wv.dispatchTouchEvent(move);

        MotionEvent up = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                1600,
                y,

                metaState
        );

        wv.dispatchTouchEvent(up);

        Log.i("chad", "running 2");
    }

    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            //Log.d("shouldOverrideUrlLoading", url);
            if (host.equals(target_url_prefix))
            {
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

            if(host.equals("m.facebook.com") || host.equals("www.facebook.com"))
            {
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch
            // another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
            Log.d("onReceivedSslError", "onReceivedSslError");
            //super.onReceivedSslError(view, handler, error);
        }

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
        if (mWebView.canGoBack()) {
        } else {
            mWebView.goBack();
            super.onBackPressed();
        }
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
            cookieManager.setAcceptCookie(true);

            mWebviewPop.setWebViewClient(new MyCustomWebViewClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setSavePassword(false);
            mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebviewPop);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.d("onCloseWindow", "called");

            mWebView.loadUrl("https://tinder.com/app/recs");
        }


        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
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

