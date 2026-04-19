package com.example.nhacnhouongnuoc;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class ReminderScheduler {
    public static final int TYPE_ALARM = 1;
    public static final int TYPE_NOTIFICATION = 2;
    public static final int REPEAT_MASK_ALL_DAYS = 0b1111111;

    private static final String PREFS_NAME = "app_settings";
    public static final String PREF_REMINDER_ITEMS = "pref_reminder_items";
    public static final String PREF_REMINDER_NEXT_ID = "pref_reminder_next_id";
    public static final String PREF_ALARM_HOUR = "pref_alarm_hour";
    public static final String PREF_ALARM_MINUTE = "pref_alarm_minute";
    public static final String PREF_NOTIFICATION_HOUR = "pref_notification_hour";
    public static final String PREF_NOTIFICATION_MINUTE = "pref_notification_minute";
    public static final String PREF_ALARM_SOUND = "pref_alarm_sound";
    public static final String PREF_NOTIFICATION_SOUND = "pref_notification_sound";
    public static final String PREF_REPEAT_DAYS_MASK = "pref_repeat_days_mask";
    public static final String DEFAULT_ALARM_SOUND = "alarm_oppo";

    private ReminderScheduler() {
    }

    public static class ReminderItem {
        public int id;
        public int hour;
        public int minute;
        public int type;
        public boolean enabled;
        public int repeatMask = REPEAT_MASK_ALL_DAYS;
        public String alarmSound = DEFAULT_ALARM_SOUND;

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("hour", hour);
            object.put("minute", minute);
            object.put("type", type);
            object.put("enabled", enabled);
            object.put("repeatMask", normalizeRepeatMask(repeatMask));
            object.put("alarmSound", alarmSound);
            return object;
        }

        static ReminderItem fromJson(JSONObject object) {
            ReminderItem item = new ReminderItem();
            item.id = object.optInt("id", 0);
            item.hour = object.optInt("hour", 8);
            item.minute = object.optInt("minute", 0);
            item.type = object.optInt("type", TYPE_NOTIFICATION);
            item.enabled = object.optBoolean("enabled", true);
            item.repeatMask = normalizeRepeatMask(object.optInt("repeatMask", REPEAT_MASK_ALL_DAYS));
            item.alarmSound = object.optString("alarmSound", DEFAULT_ALARM_SOUND);
            if (TextUtils.isEmpty(item.alarmSound)) {
                item.alarmSound = DEFAULT_ALARM_SOUND;
            }
            return item;
        }
    }

    public static List<ReminderItem> getReminderItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyRemindersIfNeeded(prefs);

        String raw = prefs.getString(PREF_REMINDER_ITEMS, "[]");
        List<ReminderItem> result = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return result;
        }

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                ReminderItem item = ReminderItem.fromJson(object);
                if (item.id <= 0) {
                    continue;
                }
                if (item.type != TYPE_ALARM && item.type != TYPE_NOTIFICATION) {
                    continue;
                }
                if (item.hour < 0 || item.hour > 23 || item.minute < 0 || item.minute > 59) {
                    continue;
                }
                result.add(item);
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static int allocateReminderId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int nextId = Math.max(1, prefs.getInt(PREF_REMINDER_NEXT_ID, 1));
        prefs.edit().putInt(PREF_REMINDER_NEXT_ID, nextId + 1).apply();
        return nextId;
    }

    public static void upsertReminder(Context context, ReminderItem item) {
        item.repeatMask = normalizeRepeatMask(item.repeatMask);
        if (TextUtils.isEmpty(item.alarmSound)) {
            item.alarmSound = DEFAULT_ALARM_SOUND;
        }
        List<ReminderItem> items = getReminderItems(context);
        boolean replaced = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == item.id) {
                items.set(i, item);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            items.add(item);
        }
        saveReminderItems(context, items);
    }

    public static void deleteReminder(Context context, int reminderId) {
        List<ReminderItem> items = getReminderItems(context);
        List<ReminderItem> remaining = new ArrayList<>();
        for (ReminderItem item : items) {
            if (item.id != reminderId) {
                remaining.add(item);
            }
        }
        saveReminderItems(context, remaining);
    }

    public static ReminderItem getReminderItemById(Context context, int reminderId) {
        for (ReminderItem item : getReminderItems(context)) {
            if (item.id == reminderId) {
                return item;
            }
        }
        return null;
    }

    public static int normalizeRepeatMask(int repeatMask) {
        int normalized = repeatMask & REPEAT_MASK_ALL_DAYS;
        return normalized == 0 ? REPEAT_MASK_ALL_DAYS : normalized;
    }

    public static int getRepeatMask(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return normalizeRepeatMask(prefs.getInt(PREF_REPEAT_DAYS_MASK, REPEAT_MASK_ALL_DAYS));
    }

    public static void updateRepeatDaysAndReschedule(Context context, int repeatMask) {
        int normalizedMask = normalizeRepeatMask(repeatMask);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_REPEAT_DAYS_MASK, normalizedMask).apply();

        // Keep this API for backward compatibility by applying global repeat to all existing reminders.
        List<ReminderItem> items = getReminderItems(context);
        for (ReminderItem item : items) {
            item.repeatMask = normalizedMask;
        }
        saveReminderItems(context, items);
        rescheduleFromStorage(context);
    }

    private static void saveReminderItems(Context context, List<ReminderItem> items) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        JSONArray array = new JSONArray();
        int maxId = 0;
        for (ReminderItem item : items) {
            maxId = Math.max(maxId, item.id);
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
            }
        }

        prefs.edit()
                .putString(PREF_REMINDER_ITEMS, array.toString())
                .putInt(PREF_REMINDER_NEXT_ID, Math.max(maxId + 1, prefs.getInt(PREF_REMINDER_NEXT_ID, 1)))
                .apply();

        rescheduleFromStorage(context);
    }

    private static void scheduleReminderInternal(Context context, ReminderItem item, int repeatMask) {
        int effectiveMask = item.repeatMask != 0 ? item.repeatMask : repeatMask;
        int normalizedMask = normalizeRepeatMask(effectiveMask);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        long triggerAtMillis = findNextTriggerTimeMillis(item.hour, item.minute, normalizedMask);
        PendingIntent pendingIntent = buildReminderPendingIntent(context, item, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void rescheduleFromStorage(Context context) {
        cancelAllReminders(context);

        int repeatMask = getRepeatMask(context);
        for (ReminderItem item : getReminderItems(context)) {
            if (!item.enabled) {
                continue;
            }
            scheduleReminderInternal(context, item, repeatMask);
        }
    }

    public static void rescheduleReminderById(Context context, int reminderId) {
        int repeatMask = getRepeatMask(context);
        for (ReminderItem item : getReminderItems(context)) {
            if (item.id != reminderId || !item.enabled) {
                continue;
            }
            scheduleReminderInternal(context, item, repeatMask);
            return;
        }
    }

    public static void cancelAllReminders(Context context) {
        for (ReminderItem item : getReminderItems(context)) {
            cancelReminder(context, item.id);
        }

        // Cancel legacy fixed request-codes from previous versions.
        cancelReminder(context, TYPE_ALARM);
        cancelReminder(context, TYPE_NOTIFICATION);
    }

    private static void cancelReminder(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildReminderPendingIntentWithRequestCode(
                context,
                requestCode,
                TYPE_NOTIFICATION,
                PendingIntent.FLAG_NO_CREATE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static long findNextTriggerTimeMillis(int hour, int minute, int repeatMask) {
        Calendar now = Calendar.getInstance();
        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            Calendar candidate = (Calendar) now.clone();
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            if (dayOffset > 0) {
                candidate.add(Calendar.DAY_OF_YEAR, dayOffset);
            }

            if (!isDayEnabled(candidate.get(Calendar.DAY_OF_WEEK), repeatMask)) {
                continue;
            }
            if (candidate.getTimeInMillis() <= now.getTimeInMillis()) {
                continue;
            }
            return candidate.getTimeInMillis();
        }

        Calendar fallback = Calendar.getInstance();
        fallback.add(Calendar.DAY_OF_YEAR, 1);
        fallback.set(Calendar.SECOND, 0);
        fallback.set(Calendar.MILLISECOND, 0);
        fallback.set(Calendar.HOUR_OF_DAY, hour);
        fallback.set(Calendar.MINUTE, minute);
        return fallback.getTimeInMillis();
    }

    private static boolean isDayEnabled(int calendarDayOfWeek, int repeatMask) {
        int bitIndex;
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY:
                bitIndex = 0;
                break;
            case Calendar.TUESDAY:
                bitIndex = 1;
                break;
            case Calendar.WEDNESDAY:
                bitIndex = 2;
                break;
            case Calendar.THURSDAY:
                bitIndex = 3;
                break;
            case Calendar.FRIDAY:
                bitIndex = 4;
                break;
            case Calendar.SATURDAY:
                bitIndex = 5;
                break;
            case Calendar.SUNDAY:
                bitIndex = 6;
                break;
            default:
                return false;
        }
        return (repeatMask & (1 << bitIndex)) != 0;
    }

    public static PendingIntent buildReminderPendingIntent(Context context, ReminderItem item, int flags) {
        return buildReminderPendingIntentWithRequestCode(context, item.id, item.type, flags);
    }

    private static PendingIntent buildReminderPendingIntentWithRequestCode(
            Context context,
            int requestCode,
            int type,
            int flags
    ) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_FIRE_REMINDER);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, type);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_ID, requestCode);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void migrateLegacyRemindersIfNeeded(SharedPreferences prefs) {
        if (prefs.contains(PREF_REMINDER_ITEMS)) {
            return;
        }

        List<ReminderItem> migrated = new ArrayList<>();
        int nextId = 1;

        int alarmHour = prefs.getInt(PREF_ALARM_HOUR, -1);
        int alarmMinute = prefs.getInt(PREF_ALARM_MINUTE, -1);
        if (alarmHour >= 0 && alarmMinute >= 0) {
            ReminderItem alarm = new ReminderItem();
            alarm.id = nextId++;
            alarm.hour = alarmHour;
            alarm.minute = alarmMinute;
            alarm.type = TYPE_ALARM;
            alarm.enabled = true;
            alarm.repeatMask = normalizeRepeatMask(prefs.getInt(PREF_REPEAT_DAYS_MASK, REPEAT_MASK_ALL_DAYS));
            alarm.alarmSound = prefs.getString(PREF_ALARM_SOUND, DEFAULT_ALARM_SOUND);
            migrated.add(alarm);
        }

        int notificationHour = prefs.getInt(PREF_NOTIFICATION_HOUR, -1);
        int notificationMinute = prefs.getInt(PREF_NOTIFICATION_MINUTE, -1);
        if (notificationHour >= 0 && notificationMinute >= 0) {
            ReminderItem noti = new ReminderItem();
            noti.id = nextId++;
            noti.hour = notificationHour;
            noti.minute = notificationMinute;
            noti.type = TYPE_NOTIFICATION;
            noti.enabled = true;
            noti.repeatMask = normalizeRepeatMask(prefs.getInt(PREF_REPEAT_DAYS_MASK, REPEAT_MASK_ALL_DAYS));
            noti.alarmSound = DEFAULT_ALARM_SOUND;
            migrated.add(noti);
        }

        JSONArray array = new JSONArray();
        for (ReminderItem item : migrated) {
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
            }
        }

        prefs.edit()
                .putString(PREF_REMINDER_ITEMS, array.toString())
                .putInt(PREF_REMINDER_NEXT_ID, nextId)
                .apply();
    }
}
