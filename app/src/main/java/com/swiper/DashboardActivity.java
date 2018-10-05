package com.swiper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.swiper.messaging.MessageEvents;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DashboardActivity extends AppCompatActivity {
    @BindView(R.id.swipeCount)
    TextView swipeCount;

    @BindView(R.id.playPauseImg)
    ImageView playPauseImg;

    boolean isPlay = false;

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_dashboard);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ButterKnife.bind(this);

        playPauseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlay) {
                    isPlay = false;
                    playPauseImg.setBackground(getDrawable(R.drawable.play));
                    EventBus.getDefault().post(new MessageEvents.PauseEvent());
                } else {
                    isPlay = true;
                    playPauseImg.setBackground(getDrawable(R.drawable.pause));
                    EventBus.getDefault().post(new MessageEvents.PlayEvent());
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvents.SwipeEvent event) {
        swipeCount.setText(String.valueOf(event.count));
    }
}

