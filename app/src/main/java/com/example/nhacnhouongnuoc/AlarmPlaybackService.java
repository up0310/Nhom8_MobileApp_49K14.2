package com.example.nhacnhouongnuoc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AlarmPlaybackService extends Service {
    public static final String ACTION_START_ALARM = "com.example.nhacnhouongnuoc.ACTION_START_ALARM";
    public static final String ACTION_STOP_ALARM = "com.example.nhacnhouongnuoc.ACTION_STOP_ALARM";
    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";

    private static final String CHANNEL_ID = "water_alarm_foreground";
    private static final int NOTIFICATION_ID = 2202;

    private MediaPlayer mediaPlayer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START_ALARM;
        if (ACTION_STOP_ALARM.equals(action)) {
            stopSelfSafely();
            return START_NOT_STICKY;
        }

        int reminderId = intent != null ? intent.getIntExtra(EXTRA_REMINDER_ID, -1) : -1;

        ensureChannel();
        startForeground(NOTIFICATION_ID, buildForegroundNotification(reminderId));
        playSelectedAlarmSound(reminderId);
        return START_STICKY;
    }

    private Notification buildForegroundNotification(int reminderId) {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                3001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, AlarmPlaybackService.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                3002,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

            Intent fullScreenIntent = new Intent(this, AlarmAlertActivity.class);
            fullScreenIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                3003,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_water_drop)
                .setContentTitle(getString(R.string.alarm_firing_title))
                .setContentText(getString(R.string.alarm_firing_text))
                .setContentIntent(openPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(0, getString(R.string.action_stop_alarm), stopPendingIntent)
                .build();
    }

    private void playSelectedAlarmSound(int reminderId) {
        stopMediaPlayer();

        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String soundValue = null;
        if (reminderId > 0) {
            ReminderScheduler.ReminderItem item = ReminderScheduler.getReminderItemById(this, reminderId);
            if (item != null && !android.text.TextUtils.isEmpty(item.alarmSound)) {
                soundValue = item.alarmSound;
            }
        }
        if (android.text.TextUtils.isEmpty(soundValue)) {
            soundValue = prefs.getString(ReminderScheduler.PREF_ALARM_SOUND, ReminderScheduler.DEFAULT_ALARM_SOUND);
        }
        int soundResId = mapSoundToRes(soundValue);

        mediaPlayer = MediaPlayer.create(this, soundResId);
        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.setLooping(true);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
        );
        mediaPlayer.start();
    }

    private int mapSoundToRes(String soundValue) {
        if ("alarm_oppo".equals(soundValue) || "alarm_soft".equals(soundValue)) {
            return R.raw.alarm_oppo;
        }
        if ("alarm_pokemon".equals(soundValue) || "alarm_bell".equals(soundValue)) {
            return R.raw.alarm_pokemon;
        }
        if ("alarm_retro".equals(soundValue) || "alarm_digital".equals(soundValue)) {
            return R.raw.alarm_retro;
        }
        if ("notification_sound".equals(soundValue)) {
            return R.raw.notification_sound;
        }
        return R.raw.alarm_oppo;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
        if (channel != null && channel.getSound() != null) {
            manager.deleteNotificationChannel(CHANNEL_ID);
            channel = null;
        }
        if (channel != null) {
            return;
        }

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel newChannel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        newChannel.setDescription(getString(R.string.alarm_channel_desc));
        newChannel.setSound(null, attributes);
        manager.createNotificationChannel(newChannel);
    }

    private void stopSelfSafely() {
        stopMediaPlayer();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void stopMediaPlayer() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    public void onDestroy() {
        stopMediaPlayer();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
