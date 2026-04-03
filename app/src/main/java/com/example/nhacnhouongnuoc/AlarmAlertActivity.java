package com.example.nhacnhouongnuoc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmAlertActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_alarm_alert);

        findViewById(R.id.btn_stop_alarm_fullscreen).setOnClickListener(v -> {
            Intent stopIntent = new Intent(this, AlarmPlaybackService.class);
            stopIntent.setAction(AlarmPlaybackService.ACTION_STOP_ALARM);
            startService(stopIntent);
            finish();
        });
    }
}
