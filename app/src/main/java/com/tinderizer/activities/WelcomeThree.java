package com.tinderizer.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.pddstudio.preferences.encrypted.EncryptedPreferences;
import com.tinderizer.R;
import com.tinderizer.activities.WelcomeOne;
import com.tinderizer.events.MessageEvents;
import com.tinderizer.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WelcomeThree extends AppCompatActivity {
    private static final String TAG = WelcomeOne.class.getSimpleName();
    private static final int FACEBOOK_CALLBACK = 0;
    private final String FIRST_RUN_KEY = "FIRST_RUN_KEY";
    private final int REQUEST_FINE_LOCATION_CODE = 0;

    @BindView(R.id.nextThreeButton)
    Button nextThreeButton;

    // Facebook
    CallbackManager facebookCallbackManager;

    private String deviceID;
    private EncryptedPreferences encryptedPreferences;

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.CloseLoading event) {
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_three);

        ButterKnife.bind(this);

        facebookCallbackManager = CallbackManager.Factory.create();

//        try {
//            PackageInfo info = getPackageManager().getPackageInfo("com.tinderizer",
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//
//        } catch (NoSuchAlgorithmException e) {
//
//        }

        //logout
        nextThreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceID = Utils.getDeviceID(getApplicationContext());
                encryptedPreferences = new EncryptedPreferences.Builder(getApplicationContext()).withEncryptionPassword(deviceID).build();

                //not first run
                encryptedPreferences.edit()
                        .putBoolean(FIRST_RUN_KEY, false)
                        .apply();

                Intent myIntent = new Intent(WelcomeThree.this, MainActivity.class);
                startActivity(myIntent);

                finish();
            }
        });

        LoginManager.getInstance().registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Log.i(TAG, "FB onSuccess()");

                Intent myIntent = new Intent(WelcomeThree.this, MainActivity.class);
                startActivity(myIntent);
            }

            @Override
            public void onCancel() {
                // App code
                Log.i(TAG, "FB onCancel()");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.i(TAG, "FB onError()");
            }
        });
    }

    // Facebook callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}

