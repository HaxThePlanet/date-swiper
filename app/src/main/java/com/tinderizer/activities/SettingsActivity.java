package com.tinderizer.activities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.BuildConfig;
import com.tinderizer.R;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

public class SettingsActivity extends AppCompatActivity implements PurchasesUpdatedListener {
    // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED = -1;
    private static final String TAG = "BillingManager";
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlXMysh/BZaZV0trE7UU+Vm5mrkJssmrBYfCHnpE9rQELfwBcwhQuIzSpTAENJgVCU+wK5h2J0ffG7v8GMfRMJ/xiEFoG73EMNSkuKVszpEuGI+UIwQQNg537xPFBWcDWrm43RIP0/v+HPCYd9p4oWSmepH0Gu9hiGQF+XBPY0xYdp2WohAuSJAXjiQmxjU8QhYZKJ9fs5LVxZwsM4MP9W7RTiLvcCETBt3/O/RRmu92pbZ2GkmLbFmDhifYXK/mpNbSSIplrbpSi2XnhyUYMwNiUUh3XCvsvz6fk2W2CdTyIWDTjiPHKkzZbUyQ5OXOOyoC5madiUs1TR2BFB5elLwIDAQAB";
    private static boolean launchPurchase;
    private final String FAST_SWIPE_KEY = "FAST_SWIPE_KEY";
    private final String NOTIF_KEY = "NOTIF_KEY";
    private final String BG_KEY = "BG_KEY";
    private final List<Purchase> mPurchases = new ArrayList<>();

    @BindView(R.id.changeLocation)
    ImageButton changeLocation;

    @BindView(R.id.logoutButton)
    Button logoutButton;

    @BindView(R.id.upgradeButton)
    Button upgradeButton;

    @BindView(R.id.notifSwitch)
    Switch notifSwitch;

    @BindView(R.id.buy_layout)
    LinearLayout buyLayout;

    @BindView(R.id.fastSwipeSwitch)
    Switch fastSwipeSwitch;

    @BindView(R.id.backgroundSwipe)
    Switch backgroundSwipe;

    @BindView(R.id.versionTextview)
    TextView versionTextview;

    String deviceID;
    EncryptedPreferences encryptedPreferences;

    /**
     * A reference to BillingClient
     **/
    private BillingClient mBillingClient;
    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;
    /**
     * True if billing service is connected now.
     */
    private boolean mIsServiceConnected;

    @Override
    public void onBackPressed() {
        EventBus.getDefault().post(new MessageEvents.CheckBilling());

        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().post(new MessageEvents.CheckBilling());
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        deviceID = Utils.getDeviceID(this);
        encryptedPreferences = new EncryptedPreferences.Builder(this).withEncryptionPassword(deviceID).build();

        setupSwitches();

        versionTextview.setText(BuildConfig.VERSION_NAME);

        //change location
        changeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder
                        .setTitle("Change location?")
                        .setMessage("Date Swiper will exit, you will need to change your location in the dating app")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(Utils.getComPackage());
                                if (launchIntent != null) {
                                    startActivity(launchIntent);

                                    //exit
                                    System.exit(1);
                                } else {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                                    alert.setTitle("");
                                    alert.setMessage("Dating app not installed");
                                    alert.setPositiveButton("OK", null);
                                    alert.show();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        //logout
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder
                        .setTitle("Are you sure?")
                        .setMessage("Logout of Date Swiper? Date Swiper will close, you will need to restart the app")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                logoutDatingApp();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        backgroundSwipe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                encryptedPreferences.edit()
                        .putBoolean(BG_KEY, isChecked)
                        .apply();
            }
        });

        notifSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                encryptedPreferences.edit()
                        .putBoolean(NOTIF_KEY, isChecked)
                        .apply();
            }
        });

        fastSwipeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                encryptedPreferences.edit()
                        .putBoolean(FAST_SWIPE_KEY, isChecked)
                        .apply();
            }
        });

        fastSwipeSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!Utils.isPurchased()) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                        builder
                                .setTitle("Upgrade")
                                .setMessage("Upgrade to paid version for faster swiping")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        initiatePurchaseFlow("date_swiper_pro_monthly_initial", INAPP);
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                    }

                    //block
                    return true;
                } else {
                    //allow
                    return false;
                }
            }
        });

        backgroundSwipe.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!Utils.isPurchased()) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                        builder
                                .setTitle("Upgrade")
                                .setMessage("Upgrade to paid version for background swiping")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        initiatePurchaseFlow("date_swiper_pro_monthly_initial", INAPP);
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                    }

                    //block
                    return true;
                } else {
                    //allow
                    return false;
                }
            }
        });

        //setup billing
        mBillingClient = BillingClient.newBuilder(SettingsActivity.this).setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    Log.i("chad", "");

                    queryPurchases();

                    if (launchPurchase)
                        initiatePurchaseFlow("date_swiper_pro_monthly_initial", INAPP);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.i("chad", "");
            }
        });

        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiatePurchaseFlow("date_swiper_pro_monthly_initial", INAPP);
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String upgrade = intent.getStringExtra("open_upgrade");
            if (upgrade != null && upgrade.equals("open_upgrade")) {
                launchPurchase = true;
            } else {
                launchPurchase = false;
            }
        }

        if (Utils.isPurchased()) {
            buyLayout.setVisibility(View.INVISIBLE);
        } else {
            buyLayout.setVisibility(View.VISIBLE);
        }
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
                mBillingClient.launchBillingFlow(SettingsActivity.this, purchaseParams);
            }
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("INFO", "chad");
        queryPurchases();

//        if (requestCode == REQUEST_CODE_RECOVER_PLAY_SERVICES) {
//            if (resultCode == RESULT_CANCELED) {
////                Toast.makeText(this, "Google Play Services must be installed.", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        } else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
//            // LOAD FROM CAMERA
////            ivf.LoadImage(tmpFilePath, quality, resolution, format);
//
//        } else if (requestCode == BUY_PREMIUM && resultCode == Activity.RESULT_OK) {
//
////            this.onPremiumPurchase();
//
//        } else if( requestCode == RC_REQUEST) {
//            if (mHelper == null) {
////                Toast.makeText(mContext, "mHelper is NULL", Toast.LENGTH_LONG).show();
//                return;
//            }
//            // Pass on the activity result to the helper for handling
////            mHelper.handleActivityResult(requestCode, resultCode, data);
//        }

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

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     *
     * @param purchase Purchase to be handled
     */
    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Log.i(TAG, "Got a purchase: " + purchase + "; but signature is bad. Skipping...");

            EventBus.getDefault().post(new MessageEvents.AppNotPurchased());
            Utils.setPurchased(false);

            return;
        }

        Log.d(TAG, "Got a verified purchase: " + purchase);

//        //restart app for state change?
//        if (!Utils.isPurchased()) {
//            android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
//            builder
//                    .setTitle("Thank You!")
//                    .setMessage("Thanks for purchasing Date Swiper, restarting app now to enable features")
//                    .setIcon(android.R.drawable.ic_dialog_info)
//                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            //restart
//                            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName());
//                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            startActivity(i);
//                        }
//                    })
//                    .show();
//        }

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.AppNotPurchased event) {
        buyLayout.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.AppPurchased event) {
        buyLayout.setVisibility(View.INVISIBLE);
    }

    private void setupSwitches() {
        //purchased?
        if (!Utils.isPurchased()) {
            //no, switches off
            notifSwitch.setChecked(false);
            fastSwipeSwitch.setChecked(false);
            backgroundSwipe.setChecked(false);
        } else {
            //yes, last state
            notifSwitch.setChecked(encryptedPreferences.getBoolean(NOTIF_KEY, true));
            fastSwipeSwitch.setChecked(encryptedPreferences.getBoolean(FAST_SWIPE_KEY, false));
            backgroundSwipe.setChecked(encryptedPreferences.getBoolean(BG_KEY, false));
        }
    }

    private void logoutDatingApp() {
        //blow cache
        ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();

        //exit
        System.exit(1);
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
        super.onResume();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}

