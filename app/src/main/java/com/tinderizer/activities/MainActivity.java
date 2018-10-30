package com.tinderizer.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.appsee.Appsee;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LevelEndEvent;
import com.crashlytics.android.answers.LevelStartEvent;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.application.SwiperApplication;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener, View.OnTouchListener {
    // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED = -1;
    private static final int freeLikesCount = 100;
    private static final String TAG = MainActivity.class.getName();
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlXMysh/BZaZV0trE7UU+Vm5mrkJssmrBYfCHnpE9rQELfwBcwhQuIzSpTAENJgVCU+wK5h2J0ffG7v8GMfRMJ/xiEFoG73EMNSkuKVszpEuGI+UIwQQNg537xPFBWcDWrm43RIP0/v+HPCYd9p4oWSmepH0Gu9hiGQF+XBPY0xYdp2WohAuSJAXjiQmxjU8QhYZKJ9fs5LVxZwsM4MP9W7RTiLvcCETBt3/O/RRmu92pbZ2GkmLbFmDhifYXK/mpNbSSIplrbpSi2XnhyUYMwNiUUh3XCvsvz6fk2W2CdTyIWDTjiPHKkzZbUyQ5OXOOyoC5madiUs1TR2BFB5elLwIDAQAB";
    private static String mGeolocationOrigin;
    private static GeolocationPermissions.Callback mGeolocationCallback;
    private static int numSwipes;
    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final String FAST_SWIPE_KEY = "FAST_SWIPE_KEY";
    private final String NOTIF_KEY = "NOTIF_KEY";
    private final String TOTAL_SWIPES_KEY = "SWIPES_KEY";
    private final String BG_KEY = "BG_KEY";
    private final int REQUEST_FINE_LOCATION_CODE = 0;
    private final List<Purchase> mPurchases = new ArrayList<>();
    @BindView(R.id.webview)
    WebView webviewMain;
    SharedPreferences preferences;
    private Tracker googleTracker;
    private Handler swipeHandler = new Handler();
    private Handler billingHandler = new Handler();
    private boolean go;

    private int webviewHeight;
    private int webviewWidth;

    private boolean dashboardUp = false;
    private CookieManager cookieManager = CookieManager.getInstance();
    private HashSet likeHashMap = new HashSet();
    private WebView webviewPopup;
    private AlertDialog popWindowBuilder;

    private int slowMinSpeed = 1000;
    private int slowMaxSpeed = 3000;

    private int fastMinSpeed = 250;
    private int fastMaxSpeed = 1500;

    private String deviceID;
    private EncryptedPreferences encryptedPreferences;

    /**
     * A reference to BillingClient
     **/
    private BillingClient mBillingClient;
    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;
    /**
     * True if billing service is connected now.
     */
    private boolean mIsServiceConnected;

    private int checkBillingHowMany = 3;
    private int haveCheckedBillingTimes = 0;
    private int billingCheckEachTimeDelay = 10000;

    private Runnable checkPurchasesThread = new Runnable() {
        public void run() {
            queryPurchases();

            if (haveCheckedBillingTimes <= checkBillingHowMany) {
                //run check again
                billingHandler.postDelayed(this, billingCheckEachTimeDelay);
                haveCheckedBillingTimes += 1;
            } else {
                //dont run again
                haveCheckedBillingTimes = 0;
            }
        }
    };

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (go && !Utils.isOutOfLikes()) {

                if (Utils.shouldRefreshWebview()) {
                    //force refresh webviewMain

                    //set last success like so we dont consta refresh
                    Utils.setSwipeTime(SystemClock.uptimeMillis());

                    //refresh
                    webviewMain.loadUrl(Utils.getRecsUrl());
                } else {
                    //send touches
                    Utils.sendWebviewTouchSwipe(webviewMain, webviewHeight, webviewWidth);
                }

            }

            if (isFastSwipeEnabled()) {
                swipeHandler.postDelayed(this, Utils.getRandomNumber(fastMinSpeed, fastMaxSpeed));
            } else {
                swipeHandler.postDelayed(this, Utils.getRandomNumber(slowMinSpeed, slowMaxSpeed));
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.CheckBilling event) {
        queryPurchases();

        android.os.Handler customHandler = new android.os.Handler();
        swipeHandler.postDelayed(checkPurchasesThread, billingCheckEachTimeDelay);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.ButtonPlayEvent event) {
        go = true;

        googleTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("ButtonPlayEvent")
                .build());

        Answers.getInstance().logLevelStart(new LevelStartEvent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.ButtonPauseEvent event) {
        go = false;

        googleTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("ButtonPauseEvent")
                .build());

        Answers.getInstance().logLevelEnd(new LevelEndEvent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.StopWebview event) {
        if (!encryptedPreferences.getBoolean(BG_KEY, false)) {
            webviewMain.loadUrl("");

            if (webviewMain != null) {
                webviewMain.onPause();
            }

            if (webviewPopup != null) {
                webviewPopup.onPause();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.StartWebview event) {
        if (webviewMain != null) {
            webviewMain.onResume();
        }

        if (webviewPopup != null) {
            webviewPopup.onResume();
        }

        webviewMain.loadUrl(Utils.getRecsUrl());
    }

    private int getTotalSwipes() {
        return preferences.getInt("total_swipes", 0);
    }

    @Override
    public void onPause() {
        if (!encryptedPreferences.getBoolean(BG_KEY, false)) {
            EventBus.getDefault().post(new MessageEvents.StopWebview());

            if (webviewMain != null) {
                webviewMain.onPause();
            }

            if (webviewPopup != null) {
                webviewPopup.onPause();
            }
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        EventBus.getDefault().post(new MessageEvents.StartWebview());

        if (!encryptedPreferences.getBoolean(BG_KEY, false)) {
            if (webviewMain != null) {
                webviewMain.onResume();
            }

            if (webviewPopup != null) {
                webviewPopup.onResume();
            }
        }

        super.onResume();
    }

    @Override
    public void onDestroy() {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        deviceID = Utils.getDeviceID(this);
        encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();

        //get todays date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("yyyy-MM-dd");
        String dateTime = mdformat.format(calendar.getTime());

        //read date key + count
//      numSwipes = encryptedPreferences.getInt(mdformat.format(calendar.getTime()), 0);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        numSwipes = preferences.getInt(dateTime, 0);

        if (numSwipes == freeLikesCount) {
            //set out of likes
            Utils.setOutOfLikes(true);
        }

        setupWebview();

        //setup billing
        mBillingClient = BillingClient.newBuilder(MainActivity.this).setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    Log.i("chad", "");

                    queryPurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.i("chad", "");
            }
        });

        android.os.Handler customHandler = new android.os.Handler();
        customHandler.postDelayed(updateTimerThread, 0);

        setupAnalytics();


    }

    private void setupAnalytics() {
        //appsee

        Appsee.start();

        // Obtain the shared Tracker instance.
        SwiperApplication application = (SwiperApplication) getApplication();
        googleTracker = application.getDefaultTracker();
        googleTracker.setScreenName(TAG);
        googleTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(INAPP);
                Log.i(TAG, "Querying purchases elapsed time: " + (System.currentTimeMillis() - time) + "ms");
                // If there are subscriptions supported, we add subscription rows as well
                if (areSubscriptionsSupported()) {
                    Purchase.PurchasesResult subscriptionResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
                    Log.i(TAG, "Querying purchases and subscriptions elapsed time: " + (System.currentTimeMillis() - time) + "ms");
                    Log.i(TAG, "Querying subscriptions result code: " + subscriptionResult.getResponseCode() + " res: " + subscriptionResult.getPurchasesList().size());

                    if (subscriptionResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                        purchasesResult.getPurchasesList().addAll(subscriptionResult.getPurchasesList());
                    } else {
                        Log.e(TAG, "Got an error response trying to query subscription purchases");
                    }
                } else if (purchasesResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                    Log.i(TAG, "Skipped subscription purchases query since they are not supported");
                } else {
                    Log.w(TAG, "queryPurchases() got an error response code: " + purchasesResult.getResponseCode());
                }
                onQueryPurchasesFinished(purchasesResult);
            }
        };

        executeServiceRequest(queryToExecute);
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    public boolean areSubscriptionsSupported() {
        int responseCode = mBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        if (responseCode != BillingClient.BillingResponse.OK) {
            Log.w(TAG, "areSubscriptionsSupported() got an error response: " + responseCode);
        }
        return responseCode == BillingClient.BillingResponse.OK;
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private void onQueryPurchasesFinished(Purchase.PurchasesResult result) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || result.getResponseCode() != BillingClient.BillingResponse.OK) {
            Log.w("", "Billing client was null or result code (" + result.getResponseCode() + ") was bad - quitting");
            return;
        }

        Log.d(TAG, "Query inventory was successful.");

        // Update the UI and purchases inventory with new list of purchases
        mPurchases.clear();
        onPurchasesUpdated(BillingClient.BillingResponse.OK, result.getPurchasesList());
    }

    /**
     * Start a purchase flow
     */
    public void initiatePurchaseFlow(final String skuId, final @BillingClient.SkuType String billingType) {
        initiatePurchaseFlow(skuId, null, billingType);
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final String skuId, final ArrayList<String> oldSkus, final @BillingClient.SkuType String billingType) {
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {
                Log.d("", "Launching in-app purchase flow. Replace old SKU? " + (oldSkus != null));
                BillingFlowParams purchaseParams = BillingFlowParams.newBuilder().setSku(skuId).setType(billingType).setOldSkus(oldSkus).build();
                mBillingClient.launchBillingFlow(MainActivity.this, purchaseParams);
            }
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    public void startServiceConnection(final Runnable executeOnSuccess) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                Log.d("", "Setup finished. Response code: " + billingResponseCode);

                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    mIsServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }
                mBillingClientResponseCode = billingResponseCode;
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(int resultCode, List<Purchase> purchases) {
        if (resultCode == BillingClient.BillingResponse.OK) {
            if (purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                    //remove for more
                    break;
                }
            } else {
                EventBus.getDefault().post(new MessageEvents.AppNotPurchased());
                Utils.setPurchased(false);
            }
        } else if (resultCode == BillingClient.BillingResponse.USER_CANCELED) {
            EventBus.getDefault().post(new MessageEvents.AppNotPurchased());
            Utils.setPurchased(false);
            Log.i("chadtest", "onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        } else {
            EventBus.getDefault().post(new MessageEvents.AppNotPurchased());
            Utils.setPurchased(false);
            Log.w("chadtest", "onPurchasesUpdated() got unknown resultCode: " + resultCode);
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Log.i(TAG, "Got a purchase: " + purchase + "; but signature is bad. Skipping...");

            EventBus.getDefault().post(new MessageEvents.AppNotPurchased());
            Utils.setPurchased(false);

            return;
        }

        Log.d(TAG, "Got a verified purchase: " + purchase);

        EventBus.getDefault().post(new MessageEvents.AppPurchased());
        Utils.setPurchased(true);

        mPurchases.add(purchase);
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        if (BASE_64_ENCODED_PUBLIC_KEY.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please update your app's public key at: "
                    + "BASE_64_ENCODED_PUBLIC_KEY");
        }

//        try {
////            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
//        } catch (IOException e) {
//            Log.e(TAG, "Got an exception trying to validate a purchase: " + e);
//            return false;
//        }

        return true;
    }

    private void setupWebview() {
        WebSettings webSettings = webviewMain.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptEnabled(true);
        webviewMain.setOnTouchListener(this);
        webviewMain.getSettings().setSavePassword(true);
        webviewMain.getSettings().setSaveFormData(true);
        webviewMain.setWebViewClient(new UriWebViewClient());
        webviewMain.setWebChromeClient(new GeoWebChromeClient());
        webviewMain.getSettings().setSavePassword(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webviewMain, true);
        }

        cookieManager.setAcceptCookie(true);

        webviewMain.setWebViewClient(new WebViewClient() {
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                if (Utils.isBlockedContent(url)) {
//                    return true;
//                }
//
//                return false;
//            }
            @Override
            public void onPageFinished(WebView view, String url) {
                // hide element by class name
                view.loadUrl("javascript:(function() { " +
                        "document.getElementsByClassName('your_class_name')[0].style.display='none'; })()");
                // hide element by id
                view.loadUrl("javascript:(function() { " +
                        "document.getElementById('your_id').style.display='none';})()");
            }
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

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("total_swipes", numSwipes + 1);
                        editor.apply();
                        editor.commit();

                        EventBus.getDefault().post(new MessageEvents.SwipeEvent(numSwipes));

                        //set last success like
                        Utils.setSwipeTime(SystemClock.uptimeMillis());

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

        webviewMain.getSettings().setAllowContentAccess(true);
        webviewMain.getSettings().setDomStorageEnabled(true);
        webviewMain.getSettings().setAllowFileAccess(true);
        webviewMain.getSettings().setAllowFileAccessFromFileURLs(true);
        webviewMain.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webviewMain.getSettings().setAppCacheEnabled(true);
        webviewMain.getSettings().setDatabaseEnabled(true);
        webviewMain.getSettings().setJavaScriptEnabled(true);
        webviewMain.getSettings().setGeolocationEnabled(true);
        webviewMain.setWebChromeClient(new GeoWebChromeClient());
        webviewMain.loadUrl(Utils.getRecsUrl());

        webviewHeight = getWindowManager().getDefaultDisplay().getHeight();
        webviewWidth = getWindowManager().getDefaultDisplay().getWidth();
    }

    private boolean isFastSwipeEnabled() {
        return encryptedPreferences.getBoolean(FAST_SWIPE_KEY, false);
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
        if (webviewMain.canGoBack()) {
        } else {
            webviewMain.goBack();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //In response to the picture on the web click event by wenview touch
        float density = getResources().getDisplayMetrics().density; //Screen density
        float touchX = event.getX() / density;  //Must be divided by the density of the screen
        float touchY = event.getY() / density;

        return false;
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
            webviewPopup = new WebView(getApplicationContext());
            webviewPopup.setVerticalScrollBarEnabled(false);
            webviewPopup.setHorizontalScrollBarEnabled(false);
            webviewPopup.setWebViewClient(new UriWebViewClient());
            webviewPopup.setWebChromeClient(new GeoWebChromeClient());
            webviewPopup.getSettings().setJavaScriptEnabled(true);
            webviewPopup.getSettings().setSavePassword(true);
            webviewPopup.getSettings().setSaveFormData(true);
            popWindowBuilder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).create();
            popWindowBuilder.setTitle("");
            popWindowBuilder.setView(webviewPopup);
            popWindowBuilder.show();
            popWindowBuilder.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(webviewPopup, true);
            }

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(webviewPopup);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            try {
                webviewPopup.destroy();
            } catch (Exception e) {
            }
            try {
                popWindowBuilder.dismiss();
            } catch (Exception e) {
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // Geolocation permissions coming from this app's Manifest will only be valid for devices with
            // API_VERSION < 23. On API 23 and above, we must check for permissions, and possibly
            // ask for them.
            String perm = Manifest.permission.ACCESS_FINE_LOCATION;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(MainActivity.this, perm) == PackageManager.PERMISSION_GRANTED) {
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

