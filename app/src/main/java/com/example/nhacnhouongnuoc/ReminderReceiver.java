package com.example.nhacnhouongnuoc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_FIRE_REMINDER = "com.example.nhacnhouongnuoc.ACTION_FIRE_REMINDER";
    public static final String EXTRA_REMINDER_TYPE = "extra_reminder_type";
    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";

    public static final String CHANNEL_NOTIFICATION = "water_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_FIRE_REMINDER.equals(intent.getAction())) {
            return;
        }

        int type = intent.getIntExtra(EXTRA_REMINDER_TYPE, ReminderScheduler.TYPE_NOTIFICATION);
        int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1);

        if (type == ReminderScheduler.TYPE_ALARM) {
            Intent serviceIntent = new Intent(context, AlarmPlaybackService.class);
            serviceIntent.setAction(AlarmPlaybackService.ACTION_START_ALARM);
            serviceIntent.putExtra(AlarmPlaybackService.EXTRA_REMINDER_ID, reminderId);
            ContextCompat.startForegroundService(context, serviceIntent);

            ReminderScheduler.rescheduleReminderById(context, reminderId);
            return;
        }

        ensureNotificationChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                1002,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_water_drop)
                .setContentTitle(context.getString(R.string.notification_firing_title))
                .setContentText(context.getString(R.string.notification_firing_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent);

        NotificationManagerCompat.from(context).notify(2201, builder.build());
        playNotificationSound(context);

        ReminderScheduler.rescheduleReminderById(context, reminderId);
    }

    private void playNotificationSound(Context context) {
        MediaPlayer player = MediaPlayer.create(context, R.raw.notification_sound);
        if (player == null) {
            return;
        }

        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
        );
        player.setOnCompletionListener(MediaPlayer::release);
        player.start();
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = manager.getNotificationChannel(CHANNEL_NOTIFICATION);
        if (channel != null && channel.getSound() != null) {
            manager.deleteNotificationChannel(CHANNEL_NOTIFICATION);
            channel = null;
        }
        if (channel != null) {
            return;
        }

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel newChannel = new NotificationChannel(
                CHANNEL_NOTIFICATION,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        newChannel.setDescription(context.getString(R.string.notification_channel_desc));
        newChannel.setSound(null, attributes);
        manager.createNotificationChannel(newChannel);
    }
}
