package com.example.nhacnhouongnuoc;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "app_settings";
    private static final String PREF_DARK_MODE = "pref_dark_mode";
    private static final String PREF_LANGUAGE = "pref_language";
    private static final String PREF_WEIGHT = "pref_weight";
    private static final String PREF_HEIGHT = "pref_height";
    private static final String PREF_AGE = "pref_age";
    private static final String PREF_ACTIVITY = "pref_activity";
    private static final String PREF_CLIMATE = "pref_climate";
    private static final String PREF_RECOMMENDED_WATER = "pref_recommended_water";
    private static final String PREF_DAILY_GOAL = "pref_daily_goal";
    private static final String PREF_LOG_HISTORY = "pref_log_history";
    private static final String PREF_INTAKE_PREFIX = "pref_intake_";
    private static final String PREF_RECOMMENDED_MIN = "pref_recommended_min";
    private static final String PREF_RECOMMENDED_MAX = "pref_recommended_max";
    private static final String PREF_ONBOARDING_DONE = "pref_onboarding_done";
    private static final String PREF_LAST_TAB_ID = "pref_last_tab_id";
    private static final String PREF_GENDER = "pref_gender";
    private static final String PREF_SOUND_ENABLED = "pref_sound_enabled";
    private static final String PREF_VIBRATE_ENABLED = "pref_vibrate_enabled";
    private static final String FIREBASE_DB_URL = "https://nhacnhouongnuoc-25ba4-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final String EXPLORE_RSS_URL = "https://news.google.com/rss/search?q=drinking+water+OR+hydration&hl=en-US&gl=US&ceid=US:en";
    private static final String[] SUPPORTED_LANGUAGES = {"vi", "en"};
    private static final String[] ALARM_SOUND_VALUES = {"alarm_oppo", "alarm_pokemon", "alarm_retro", "notification_sound"};
        private static final int[] DIALOG_REPEAT_DAY_CHECKBOX_IDS = {
            R.id.cb_dialog_repeat_mon,
            R.id.cb_dialog_repeat_tue,
            R.id.cb_dialog_repeat_wed,
            R.id.cb_dialog_repeat_thu,
            R.id.cb_dialog_repeat_fri,
            R.id.cb_dialog_repeat_sat,
            R.id.cb_dialog_repeat_sun
        };
    private static final int[] WEEKDAY_LABEL_IDS = {
            R.string.weekday_mon,
            R.string.weekday_tue,
            R.string.weekday_wed,
            R.string.weekday_thu,
            R.string.weekday_fri,
            R.string.weekday_sat,
            R.string.weekday_sun
    };
    private static final long AUTO_SYNC_COOLDOWN_MS = 15_000L;
        private static final int TOOL_BODY_FAT = 1;
        private static final int TOOL_BMI = 2;
        private static final int TOOL_BMR = 3;
        private static final int TOOL_TDEE = 4;
        private static final int TOOL_DAILY_CALORIES = 5;
        private static final int TOOL_CALORIES_BURN = 6;
        private static final int TOOL_HEALTHY_WEIGHT = 7;
        private static final String FIXED_NOTIFICATION_SOUND = "notification_sound";

    private FrameLayout container;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseRoot;
    private TextView accountStatusView;
    private View accountLoginNowButton;
    private View accountChangePasswordButton;
    private View accountLogoutButton;
    private View accountGuestActions;
    private View accountLoggedInActions;
    private String lastSeenUserUid;
    private long lastAutoSyncAtMs;
    private MediaPlayer previewPlayer;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, R.string.notification_permission_needed, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeAndLanguage();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);
        firebaseAuth = FirebaseAuth.getInstance();
        databaseRoot = FirebaseDatabase.getInstance(FIREBASE_DB_URL).getReference();
        lastSeenUserUid = null;
        lastAutoSyncAtMs = 0L;

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        ReminderReceiver.ensureNotificationChannel(this);
        ReminderScheduler.rescheduleFromStorage(this);
        requestNotificationPermissionIfNeeded();

        container = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        applyEdgeToEdgeInsets(bottomNavigationView);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int selectedTabId = prefs.getInt(PREF_LAST_TAB_ID, R.id.nav_today);
        bottomNavigationView.setSelectedItemId(selectedTabId);
        showScreen(selectedTabId);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            prefs.edit().putInt(PREF_LAST_TAB_ID, item.getItemId()).apply();
            showScreen(item.getItemId());
            return true;
        });

        maybeShowFirstLaunchOnboarding();
    }

    private void applyEdgeToEdgeInsets(BottomNavigationView bottomNav) {
        View root = findViewById(R.id.main);
        final int containerLeft = container.getPaddingLeft();
        final int containerTop = container.getPaddingTop();
        final int containerRight = container.getPaddingRight();
        final int containerBottom = container.getPaddingBottom();
        final ViewGroup.MarginLayoutParams navLayoutParams =
                (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
        final int navBottomMargin = navLayoutParams.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            container.setPadding(
                    containerLeft,
                    containerTop + systemBars.top,
                    containerRight,
                    containerBottom
            );

            // Keep nav attached to bottom; adding system bar inset here creates a visible blank strip.
            navLayoutParams.bottomMargin = navBottomMargin;
            bottomNav.setLayoutParams(navLayoutParams);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void showScreen(int itemId) {
        @LayoutRes int layoutRes = getLayoutForMenu(itemId);
        if (layoutRes == 0) {
            return;
        }

        View screen = getLayoutInflater().inflate(layoutRes, container, false);
        container.removeAllViews();
        container.addView(screen);
        try {
            if (itemId == R.id.nav_today) {
                setupHomeScreen(screen);
            }

            if (itemId == R.id.nav_stats) {
                setupStatsScreen(screen);
            }

            if (itemId == R.id.nav_settings) {
                setupSettingsScreen(screen);
            }

            if (itemId == R.id.nav_explore) {
                setupExploreScreen(screen);
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Đã có lỗi hiển thị màn hình, đang tải lại.", Toast.LENGTH_SHORT).show();
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putInt(PREF_LAST_TAB_ID, R.id.nav_today).apply();
            if (itemId != R.id.nav_today) {
                bottomNavigationView.setSelectedItemId(R.id.nav_today);
                showScreen(R.id.nav_today);
            }
        }
    }

    @LayoutRes
    private int getLayoutForMenu(int itemId) {
        if (itemId == R.id.nav_today) {
            return R.layout.fragment_home;
        }
        if (itemId == R.id.nav_stats) {
            return R.layout.fragment_stats;
        }
        if (itemId == R.id.nav_explore) {
            return R.layout.fragment_explore;
        }
        if (itemId == R.id.nav_settings) {
            return R.layout.fragment_settings;
        }
        return 0;
    }

    private void setupSettingsScreen(View settingsView) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        SwitchCompat darkModeSwitch = settingsView.findViewById(R.id.switch_dark_mode);
        Spinner languageSpinner = settingsView.findViewById(R.id.spinner_language);
        View reminderItem = settingsView.findViewById(R.id.item_reminder_schedule);
        View soundItem = settingsView.findViewById(R.id.item_sound_effect);
        View profileItem = settingsView.findViewById(R.id.item_profile);
        View rateItem = settingsView.findViewById(R.id.item_rate);

        if (darkModeSwitch == null || languageSpinner == null
                || reminderItem == null || soundItem == null
                || profileItem == null || rateItem == null) {
            Toast.makeText(this, "Thiếu thành phần giao diện cài đặt.", Toast.LENGTH_SHORT).show();
            return;
        }

        darkModeSwitch.setChecked(prefs.getBoolean(PREF_DARK_MODE, false));
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean current = prefs.getBoolean(PREF_DARK_MODE, false);
            if (current == isChecked) {
                return;
            }
            prefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            syncProfileToCloudIfLoggedIn();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate();
        });

        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_display_names,
                android.R.layout.simple_spinner_item
        );
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        String currentLanguage = prefs.getString(PREF_LANGUAGE, SUPPORTED_LANGUAGES[0]);
        languageSpinner.setSelection(getLanguageIndex(currentLanguage), false);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= SUPPORTED_LANGUAGES.length) {
                    return;
                }
                String selected = SUPPORTED_LANGUAGES[position];
                String saved = prefs.getString(PREF_LANGUAGE, SUPPORTED_LANGUAGES[0]);
                if (selected.equals(saved)) {
                    return;
                }
                prefs.edit().putString(PREF_LANGUAGE, selected).apply();
                syncProfileToCloudIfLoggedIn();
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected));
                recreate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        reminderItem.setOnClickListener(v -> showReminderScheduleDialog());
        soundItem.setOnClickListener(v -> showSoundEffectsDialog(prefs));
        profileItem.setOnClickListener(v -> showProfileInfoDialog(prefs));
        rateItem.setOnClickListener(v -> showRateAppDialog());

        setupAccountAndSync(settingsView);
    }

    private void showRateAppDialog() {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_rate_app);
        View root = dialog.findViewById(android.R.id.content);

        View closeButton = root.findViewById(R.id.btn_close_rate);
        View formLayout = root.findViewById(R.id.layout_rate_form);
        View successLayout = root.findViewById(R.id.layout_rate_success);
        RatingBar inputRating = root.findViewById(R.id.rb_rate_input);
        RatingBar resultRating = root.findViewById(R.id.rb_rate_result);
        TextView rateStatus = root.findViewById(R.id.tv_rate_status);
        EditText commentInput = root.findViewById(R.id.et_rate_comment);
        View submitButton = root.findViewById(R.id.btn_submit_rate);
        View backButton = root.findViewById(R.id.btn_rate_back_settings);

        inputRating.setRating(4f);
        updateRateStatusText(rateStatus, 4);

        inputRating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            int stars = Math.max(1, Math.round(rating));
            if (rating != stars) {
                inputRating.setRating(stars);
            }
            updateRateStatusText(rateStatus, stars);
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());
        backButton.setOnClickListener(v -> dialog.dismiss());

        submitButton.setOnClickListener(v -> {
            int stars = Math.max(1, Math.round(inputRating.getRating()));
            String comment = commentInput.getText().toString().trim();

            saveRatingToCloudIfLoggedIn(stars, comment);
            resultRating.setRating(stars);

            formLayout.setVisibility(View.GONE);
            successLayout.setVisibility(View.VISIBLE);

            Bundle params = new Bundle();
            params.putInt("stars", stars);
            params.putString("comment", comment);
            firebaseAnalytics.logEvent("app_rated", params);
        });

        dialog.show();
    }

    private void updateRateStatusText(TextView statusView, int stars) {
        if (stars >= 5) {
            statusView.setText(R.string.rate_status_excellent);
        } else if (stars >= 4) {
            statusView.setText(R.string.rate_status_good);
        } else if (stars >= 3) {
            statusView.setText(R.string.rate_status_okay);
        } else {
            statusView.setText(R.string.rate_status_bad);
        }
    }

    private void saveRatingToCloudIfLoggedIn(int stars, String comment) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.sync_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> ratingMap = new HashMap<>();
        ratingMap.put("stars", stars);
        ratingMap.put("comment", comment);
        ratingMap.put("timestamp", System.currentTimeMillis());

        databaseRoot.child("users")
                .child(user.getUid())
                .child("appRatings")
                .push()
                .setValue(ratingMap);
    }

    private Dialog createFullScreenDialog(int layoutRes) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        dialog.setContentView(layoutRes);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void showReminderScheduleDialog() {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_reminder_schedule);
        View root = dialog.findViewById(android.R.id.content);
        View closeButton = dialog.findViewById(R.id.btn_close_schedule);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        setupReminderList(root);
        dialog.show();
    }

    private void showSoundEffectsDialog(SharedPreferences prefs) {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_sound_effects);
        View root = dialog.findViewById(android.R.id.content);
        View closeButton = dialog.findViewById(R.id.btn_close_sound);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        SwitchCompat soundSwitch = root.findViewById(R.id.switch_sound_enabled);
        SwitchCompat vibrateSwitch = root.findViewById(R.id.switch_vibrate_enabled);
        Spinner alarmSoundSpinner = root.findViewById(R.id.spinner_alarm_sound);
        View previewAlarmButton = root.findViewById(R.id.btn_preview_alarm_sound);
        View stopPreviewAlarmButton = root.findViewById(R.id.btn_stop_preview_alarm_sound);

        soundSwitch.setChecked(prefs.getBoolean(PREF_SOUND_ENABLED, true));
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_SOUND_ENABLED, isChecked).apply();
            syncProfileToCloudIfLoggedIn();
        });

        vibrateSwitch.setChecked(prefs.getBoolean(PREF_VIBRATE_ENABLED, true));
        vibrateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_VIBRATE_ENABLED, isChecked).apply();
            syncProfileToCloudIfLoggedIn();
        });

        ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.alarm_sound_labels,
                android.R.layout.simple_spinner_item
        );
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alarmSoundSpinner.setAdapter(soundAdapter);

        String selectedSound = prefs.getString(ReminderScheduler.PREF_ALARM_SOUND, ALARM_SOUND_VALUES[0]);
        alarmSoundSpinner.setSelection(findSoundIndex(selectedSound), false);
        alarmSoundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= ALARM_SOUND_VALUES.length) {
                    return;
                }
                String newValue = ALARM_SOUND_VALUES[position];
                if (!newValue.equals(prefs.getString(ReminderScheduler.PREF_ALARM_SOUND, ALARM_SOUND_VALUES[0]))) {
                    prefs.edit().putString(ReminderScheduler.PREF_ALARM_SOUND, newValue).apply();
                    syncProfileToCloudIfLoggedIn();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        previewAlarmButton.setOnClickListener(v ->
                playPreviewSound(ALARM_SOUND_VALUES[Math.max(0, Math.min(ALARM_SOUND_VALUES.length - 1,
                        alarmSoundSpinner.getSelectedItemPosition()))])
        );
        stopPreviewAlarmButton.setOnClickListener(v -> stopPreviewSound());
        dialog.show();
    }

    private void showProfileInfoDialog(SharedPreferences prefs) {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_profile_info);
        View root = dialog.findViewById(android.R.id.content);

        View closeButton = root.findViewById(R.id.btn_close_profile);
        View saveButton = root.findViewById(R.id.btn_save_profile);
        View applyGoalButton = root.findViewById(R.id.btn_apply_water_goal);
        EditText weightInput = root.findViewById(R.id.et_weight);
        TextView recommendedView = root.findViewById(R.id.tv_recommended_water);
        TextView recommendationNoteView = root.findViewById(R.id.tv_recommended_note);

        com.google.android.material.chip.Chip maleChip = root.findViewById(R.id.chip_gender_male);
        com.google.android.material.chip.Chip femaleChip = root.findViewById(R.id.chip_gender_female);
        com.google.android.material.chip.Chip otherChip = root.findViewById(R.id.chip_gender_other);
        com.google.android.material.chip.ChipGroup genderGroup = root.findViewById(R.id.chip_group_gender);

        weightInput.setText(prefs.getString(PREF_WEIGHT, ""));
        applyGenderSelection(prefs.getString(PREF_GENDER, "other"), maleChip, femaleChip, otherChip);
        genderGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                applyGenderSelection("other", maleChip, femaleChip, otherChip);
            }
        });

        updateRecommendedWaterPreview(prefs, weightInput, recommendedView, recommendationNoteView);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> {
            if (saveProfileFromDialog(prefs, weightInput, maleChip, femaleChip, otherChip,
                    recommendedView, recommendationNoteView)) {
                refreshActiveScreen();
                dialog.dismiss();
            }
        });

        applyGoalButton.setOnClickListener(v -> {
            if (saveProfileFromDialog(prefs, weightInput, maleChip, femaleChip, otherChip,
                    recommendedView, recommendationNoteView)) {
                refreshActiveScreen();
                Toast.makeText(this, R.string.reminder_saved, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private boolean saveProfileFromDialog(SharedPreferences prefs,
                                          EditText weightInput,
                                          com.google.android.material.chip.Chip maleChip,
                                          com.google.android.material.chip.Chip femaleChip,
                                          com.google.android.material.chip.Chip otherChip,
                                          TextView recommendedView,
                                          TextView recommendationNoteView) {
        Double weight = parsePositiveDouble(weightInput.getText().toString().trim());
        if (weight == null) {
            Toast.makeText(this, R.string.input_required, Toast.LENGTH_SHORT).show();
            return false;
        }

        Integer age = parsePositiveInt(prefs.getString(PREF_AGE, "25"));
        int safeAge = age != null ? age : 25;
        int activityLevel = prefs.getInt(PREF_ACTIVITY, 0);
        WaterRecommendation recommendation = calculateRecommendedWater(weight, safeAge, activityLevel);

        prefs.edit()
                .putString(PREF_WEIGHT, weightInput.getText().toString().trim())
                .putString(PREF_GENDER, getSelectedGender(maleChip, femaleChip, otherChip))
                .putInt(PREF_RECOMMENDED_WATER, recommendation.targetMl)
                .putInt(PREF_RECOMMENDED_MIN, recommendation.minMl)
                .putInt(PREF_RECOMMENDED_MAX, recommendation.maxMl)
                .putInt(PREF_DAILY_GOAL, recommendation.targetMl)
                .putBoolean(PREF_ONBOARDING_DONE, true)
                .apply();

            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Map<String, Object> quickProfile = new HashMap<>();
                quickProfile.put("weight", weightInput.getText().toString().trim());
                quickProfile.put("gender", getSelectedGender(maleChip, femaleChip, otherChip));
                quickProfile.put("updatedAt", System.currentTimeMillis());
                databaseRoot.child("users")
                    .child(user.getUid())
                    .child("profile")
                    .updateChildren(quickProfile);
            }

        recommendedView.setText(getString(R.string.recommended_water_big, recommendation.targetMl));
        recommendationNoteView.setText(getString(
                R.string.recommended_water_note,
                weight,
                recommendation.targetMl
        ));
        syncProfileToCloudIfLoggedIn();
        return true;
    }

    private void updateRecommendedWaterPreview(SharedPreferences prefs,
                                               EditText weightInput,
                                               TextView recommendedView,
                                               TextView recommendationNoteView) {
        Double weight = parsePositiveDouble(weightInput.getText().toString().trim());
        if (weight == null) {
            int savedWater = prefs.getInt(PREF_RECOMMENDED_WATER, 2500);
            recommendedView.setText(getString(R.string.recommended_water_big, savedWater));
            recommendationNoteView.setText(getString(
                    R.string.recommended_water_note,
                    70.0,
                    savedWater
            ));
            return;
        }

        Integer age = parsePositiveInt(prefs.getString(PREF_AGE, "25"));
        int safeAge = age != null ? age : 25;
        int activityLevel = prefs.getInt(PREF_ACTIVITY, 0);
        WaterRecommendation recommendation = calculateRecommendedWater(weight, safeAge, activityLevel);
        recommendedView.setText(getString(R.string.recommended_water_big, recommendation.targetMl));
        recommendationNoteView.setText(getString(
                R.string.recommended_water_note,
                weight,
                recommendation.targetMl
        ));
    }

    private void applyGenderSelection(String gender,
                                      com.google.android.material.chip.Chip maleChip,
                                      com.google.android.material.chip.Chip femaleChip,
                                      com.google.android.material.chip.Chip otherChip) {
        maleChip.setChecked("male".equals(gender));
        femaleChip.setChecked("female".equals(gender));
        otherChip.setChecked(!"male".equals(gender) && !"female".equals(gender));
    }

    private String getSelectedGender(com.google.android.material.chip.Chip maleChip,
                                     com.google.android.material.chip.Chip femaleChip,
                                     com.google.android.material.chip.Chip otherChip) {
        if (maleChip.isChecked()) {
            return "male";
        }
        if (femaleChip.isChecked()) {
            return "female";
        }
        if (otherChip.isChecked()) {
            return "other";
        }
        return "other";
    }

    private void setupAccountAndSync(View settingsView) {
        TextView accountStatus = settingsView.findViewById(R.id.tv_account_status);
        accountStatusView = accountStatus;
        View loginNowButton = settingsView.findViewById(R.id.btn_login_now);
        View changePasswordButton = settingsView.findViewById(R.id.btn_change_password);
        View logoutButton = settingsView.findViewById(R.id.btn_logout);
        View guestActions = settingsView.findViewById(R.id.layout_guest_actions);
        View loggedInActions = settingsView.findViewById(R.id.layout_logged_in_actions);

        accountLoginNowButton = loginNowButton;
        accountChangePasswordButton = changePasswordButton;
        accountLogoutButton = logoutButton;
        accountGuestActions = guestActions;
        accountLoggedInActions = loggedInActions;

        updateAccountStatusLabel(accountStatus);
        updateAccountActionState();

        loginNowButton.setOnClickListener(v -> openAuthScreen());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        logoutButton.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(this, R.string.account_not_logged_in, Toast.LENGTH_SHORT).show();
                return;
            }
            firebaseAuth.signOut();
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            clearLocalAccountScopedData(prefs);
            ReminderScheduler.cancelAllReminders(this);
            lastSeenUserUid = null;
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            updateAccountStatusLabel(accountStatus);
            updateAccountActionState();
            openAuthScreen();
        });
    }

    private void updateAccountActionState() {
        boolean isLoggedIn = firebaseAuth.getCurrentUser() != null;
        if (accountGuestActions != null) {
            accountGuestActions.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        }
        if (accountLoggedInActions != null) {
            accountLoggedInActions.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }
        if (accountLoginNowButton != null) {
            accountLoginNowButton.setEnabled(!isLoggedIn);
            accountLoginNowButton.setAlpha(isLoggedIn ? 0.5f : 1f);
        }
        if (accountChangePasswordButton != null) {
            accountChangePasswordButton.setEnabled(isLoggedIn);
            accountChangePasswordButton.setAlpha(isLoggedIn ? 1f : 0.5f);
        }
        if (accountLogoutButton != null) {
            accountLogoutButton.setEnabled(isLoggedIn);
            accountLogoutButton.setAlpha(isLoggedIn ? 1f : 0.5f);
        }
    }

    private void openAuthScreen() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void openRegisterScreen() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            Toast.makeText(this, R.string.account_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null, false);
        EditText currentPasswordInput = dialogView.findViewById(R.id.et_current_password);
        EditText newPasswordInput = dialogView.findViewById(R.id.et_new_password);
        EditText confirmPasswordInput = dialogView.findViewById(R.id.et_confirm_new_password);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
            .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (TextUtils.isEmpty(currentPassword)
                    || TextUtils.isEmpty(newPassword)
                    || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, R.string.input_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) {
                Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, R.string.password_not_match, Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
            user.reauthenticate(credential)
                    .addOnSuccessListener(unused -> user.updatePassword(newPassword)
                            .addOnSuccessListener(unusedPassword -> {
                                Toast.makeText(this, R.string.password_updated_success, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
//                                navigateUpTo()
                            })
                            .addOnFailureListener(ex -> Toast.makeText(
                                    this,
                                    getString(R.string.auth_failed, ex == null ? "" : String.valueOf(ex.getMessage())),
                                    Toast.LENGTH_SHORT
                            ).show())
                    )
                    .addOnFailureListener(ex -> Toast.makeText(
                            this,
                            getString(R.string.current_password_incorrect),
                            Toast.LENGTH_SHORT
                    ).show());
        });
    }

    private void updateAccountStatusLabel(TextView label) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            label.setText(R.string.account_not_logged_in);
            return;
        }
        label.setText(getString(R.string.account_logged_in_as, user.getEmail()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncAccountDataIfUserChanged();
        autoSyncIfLoggedIn();
        if (accountStatusView != null) {
            updateAccountStatusLabel(accountStatusView);
        }
        updateAccountActionState();
    }

    private void autoSyncIfLoggedIn() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAutoSyncAtMs < AUTO_SYNC_COOLDOWN_MS) {
            return;
        }

        lastAutoSyncAtMs = now;
        syncCloudToLocalThenRefresh();
    }

    private void syncAccountDataIfUserChanged() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String currentUid = currentUser == null ? null : currentUser.getUid();

        if (currentUid == null) {
            lastSeenUserUid = null;
            return;
        }

        if (currentUid.equals(lastSeenUserUid)) {
            return;
        }

        lastSeenUserUid = currentUid;
        DatabaseReference userRef = databaseRoot.child("users").child(currentUid);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasOfflineData = hasLocalAccountData(prefs);

        userRef.get().addOnSuccessListener(snapshot -> {
            boolean hasProfile = snapshot.hasChild("profile") && snapshot.child("profile").hasChildren();
            boolean hasDaily = snapshot.hasChild("daily") && snapshot.child("daily").hasChildren();
            boolean hasCloudData = hasProfile || hasDaily;

            if (hasCloudData && hasOfflineData) {
                syncLocalToCloudAndRefresh(accountStatusView);
                return;
            }

            if (hasCloudData) {
                syncCloudToLocalThenRefresh();
                return;
            }

            if (hasOfflineData) {
                syncLocalToCloudAndRefresh(accountStatusView);
            }
        });
    }

    private void showSyncChoiceDialog(SharedPreferences prefs) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sync_choice_title)
                .setMessage(R.string.sync_choice_message)
                .setPositiveButton(R.string.sync_choice_push_offline, (dialog, which) -> {
                    syncLocalToCloudAndRefresh(accountStatusView);
                    Toast.makeText(this, R.string.sync_choice_push_done, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.sync_choice_use_cloud, (dialog, which) -> {
                    clearLocalAccountScopedData(prefs);
                    syncCloudToLocalThenRefresh();
                    Toast.makeText(this, R.string.sync_choice_use_cloud_done, Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private boolean hasLocalAccountData(SharedPreferences prefs) {
        if (!TextUtils.isEmpty(prefs.getString(PREF_WEIGHT, ""))
                || !TextUtils.isEmpty(prefs.getString(PREF_HEIGHT, ""))
                || !TextUtils.isEmpty(prefs.getString(PREF_AGE, ""))) {
            return true;
        }

        if (!ReminderScheduler.getReminderItems(this).isEmpty()) {
            return true;
        }

        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            Calendar day = (Calendar) calendar.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            if (prefs.getInt(PREF_INTAKE_PREFIX + buildDateKey(day), 0) > 0) {
                return true;
            }
        }
        return false;
    }

    private void clearLocalAccountScopedData(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_WEIGHT);
        editor.remove(PREF_GENDER);
        editor.remove(PREF_HEIGHT);
        editor.remove(PREF_AGE);
        editor.remove(PREF_ACTIVITY);
        editor.remove(PREF_CLIMATE);
        editor.remove(PREF_RECOMMENDED_WATER);
        editor.remove(PREF_DAILY_GOAL);
        editor.remove(PREF_RECOMMENDED_MIN);
        editor.remove(PREF_RECOMMENDED_MAX);
        editor.remove(PREF_LOG_HISTORY);
        editor.remove(PREF_ONBOARDING_DONE);
        editor.remove(ReminderScheduler.PREF_REMINDER_ITEMS);
        editor.remove(ReminderScheduler.PREF_REMINDER_NEXT_ID);
        editor.remove(ReminderScheduler.PREF_ALARM_HOUR);
        editor.remove(ReminderScheduler.PREF_ALARM_MINUTE);
        editor.remove(ReminderScheduler.PREF_NOTIFICATION_HOUR);
        editor.remove(ReminderScheduler.PREF_NOTIFICATION_MINUTE);
        editor.remove(ReminderScheduler.PREF_ALARM_SOUND);
        editor.remove(ReminderScheduler.PREF_REPEAT_DAYS_MASK);

        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 60; i++) {
            Calendar day = (Calendar) calendar.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            editor.remove(PREF_INTAKE_PREFIX + buildDateKey(day));
        }
        editor.apply();
    }

    private void syncLocalToCloudAndRefresh(TextView accountStatus) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uid = user.getUid();
        DatabaseReference userRef = databaseRoot.child("users").child(uid);

        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("weight", prefs.getString(PREF_WEIGHT, ""));
        profileMap.put("gender", prefs.getString(PREF_GENDER, "other"));
        profileMap.put("height", prefs.getString(PREF_HEIGHT, ""));
        profileMap.put("age", prefs.getString(PREF_AGE, ""));
        profileMap.put("activity", prefs.getInt(PREF_ACTIVITY, 0));
        profileMap.put("climate", prefs.getInt(PREF_CLIMATE, 0));
        profileMap.put("dailyGoal", getDailyGoal(prefs));
        profileMap.put("recommendedMin", prefs.getInt(PREF_RECOMMENDED_MIN, 1500));
        profileMap.put("recommendedMax", prefs.getInt(PREF_RECOMMENDED_MAX, 2000));
        profileMap.put("recommendedTarget", prefs.getInt(PREF_RECOMMENDED_WATER, 2500));
        userRef.child("profile").setValue(profileMap);

        userRef.child("reminders").child("itemsJson")
            .setValue(prefs.getString(ReminderScheduler.PREF_REMINDER_ITEMS, "[]"));
        userRef.child("reminders").child("updatedAt").setValue(System.currentTimeMillis());

        userRef.child("logs").child("historyJson")
            .setValue(prefs.getString(PREF_LOG_HISTORY, "[]"));
        userRef.child("logs").child("updatedAt").setValue(System.currentTimeMillis());

        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            Calendar day = (Calendar) calendar.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = buildDateKey(day);
            int intake = prefs.getInt(PREF_INTAKE_PREFIX + dateKey, 0);
            if (intake <= 0) {
                continue;
            }

            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("intakeMl", intake);
            dayMap.put("goalMl", getDailyGoal(prefs));
            dayMap.put("updatedAt", System.currentTimeMillis());
            userRef.child("daily").child(dateKey).updateChildren(dayMap);
        }

        if (accountStatus != null) {
            updateAccountStatusLabel(accountStatus);
        }
        syncCloudToLocalThenRefresh();
    }

    private void syncCloudToLocalThenRefresh() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = databaseRoot.child("users").child(user.getUid());
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        userRef.child("profile").get().addOnSuccessListener(snapshot -> {
            SharedPreferences.Editor editor = prefs.edit();
            if (snapshot.hasChild("weight")) {
                String value = snapshot.child("weight").getValue(String.class);
                if (value != null) {
                    editor.putString(PREF_WEIGHT, value);
                }
            }
            if (snapshot.hasChild("gender")) {
                String value = snapshot.child("gender").getValue(String.class);
                if (!TextUtils.isEmpty(value)) {
                    editor.putString(PREF_GENDER, value);
                }
            }
            if (snapshot.hasChild("height")) {
                String value = snapshot.child("height").getValue(String.class);
                if (value != null) {
                    editor.putString(PREF_HEIGHT, value);
                }
            }
            if (snapshot.hasChild("age")) {
                String value = snapshot.child("age").getValue(String.class);
                if (value != null) {
                    editor.putString(PREF_AGE, value);
                }
            }
            if (snapshot.hasChild("activity")) {
                Integer value = snapshot.child("activity").getValue(Integer.class);
                if (value != null) {
                    editor.putInt(PREF_ACTIVITY, value);
                }
            }
            if (snapshot.hasChild("climate")) {
                Integer value = snapshot.child("climate").getValue(Integer.class);
                if (value != null) {
                    editor.putInt(PREF_CLIMATE, value);
                }
            }
            if (snapshot.hasChild("dailyGoal")) {
                Integer value = snapshot.child("dailyGoal").getValue(Integer.class);
                if (value != null) {
                    editor.putInt(PREF_DAILY_GOAL, value);
                }
            }
            if (snapshot.hasChild("recommendedMin")) {
                Integer value = snapshot.child("recommendedMin").getValue(Integer.class);
                if (value != null) {
                    editor.putInt(PREF_RECOMMENDED_MIN, value);
                }
            }
            if (snapshot.hasChild("recommendedMax")) {
                Integer value = snapshot.child("recommendedMax").getValue(Integer.class);
                if (value != null) {
                    editor.putInt(PREF_RECOMMENDED_MAX, value);
                }
            }
            if (snapshot.hasChild("recommendedTarget")) {
                Integer value = snapshot.child("recommendedTarget").getValue(Integer.class);
                if (value != null) {
                    editor.putInt(PREF_RECOMMENDED_WATER, value);
                }
            }
            if (snapshot.hasChild("darkMode")) {
                Boolean value = snapshot.child("darkMode").getValue(Boolean.class);
                if (value != null) {
                    editor.putBoolean(PREF_DARK_MODE, value);
                }
            }
            if (snapshot.hasChild("language")) {
                String value = snapshot.child("language").getValue(String.class);
                if (!TextUtils.isEmpty(value)) {
                    editor.putString(PREF_LANGUAGE, value);
                }
            }
            if (snapshot.hasChild("alarmSound")) {
                String value = snapshot.child("alarmSound").getValue(String.class);
                if (!TextUtils.isEmpty(value)) {
                    editor.putString(ReminderScheduler.PREF_ALARM_SOUND, value);
                }
            }
            if (snapshot.hasChild("notificationSound")) {
                String value = snapshot.child("notificationSound").getValue(String.class);
                if (!TextUtils.isEmpty(value)) {
                    editor.putString(ReminderScheduler.PREF_NOTIFICATION_SOUND, value);
                }
            }
            editor.apply();
            refreshActiveScreen();
        });

        userRef.child("daily").get().addOnSuccessListener(snapshot -> {
            SharedPreferences.Editor editor = prefs.edit();
            for (DataSnapshot day : snapshot.getChildren()) {
                String dateKey = day.getKey();
                Integer intakeMl = day.child("intakeMl").getValue(Integer.class);
                if (dateKey != null && intakeMl != null) {
                    editor.putInt(PREF_INTAKE_PREFIX + dateKey, intakeMl);
                }
            }
            editor.apply();
            refreshActiveScreen();
        });

        userRef.child("reminders").child("itemsJson").get().addOnSuccessListener(snapshot -> {
            String itemsJson = snapshot.getValue(String.class);
            if (!TextUtils.isEmpty(itemsJson)) {
                prefs.edit().putString(ReminderScheduler.PREF_REMINDER_ITEMS, itemsJson).apply();
                ReminderScheduler.rescheduleFromStorage(this);
            }
            refreshActiveScreen();
        });

        userRef.child("logs").child("historyJson").get().addOnSuccessListener(snapshot -> {
            String logsJson = snapshot.getValue(String.class);
            if (!TextUtils.isEmpty(logsJson)) {
                prefs.edit().putString(PREF_LOG_HISTORY, logsJson).apply();
            }
            refreshActiveScreen();
        });

    }

    private void syncTodayToCloudIfLoggedIn(int intakeMl, int goalMl) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        Map<String, Object> dayMap = new HashMap<>();
        dayMap.put("intakeMl", intakeMl);
        dayMap.put("goalMl", goalMl);
        dayMap.put("streakDays", getCurrentStreakDays(getSharedPreferences(PREFS_NAME, MODE_PRIVATE)));
        dayMap.put("updatedAt", System.currentTimeMillis());

        databaseRoot.child("users")
                .child(user.getUid())
                .child("daily")
                .child(getTodayKey())
                .updateChildren(dayMap);
    }

    private void syncRemindersToCloudIfLoggedIn() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseRoot.child("users")
                .child(user.getUid())
                .child("reminders")
                .child("itemsJson")
                .setValue(prefs.getString(ReminderScheduler.PREF_REMINDER_ITEMS, "[]"));
        databaseRoot.child("users")
                .child(user.getUid())
                .child("reminders")
                .child("updatedAt")
                .setValue(System.currentTimeMillis());
    }

    private void syncLogsToCloudIfLoggedIn() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseRoot.child("users")
                .child(user.getUid())
                .child("logs")
                .child("historyJson")
                .setValue(prefs.getString(PREF_LOG_HISTORY, "[]"));
        databaseRoot.child("users")
                .child(user.getUid())
                .child("logs")
                .child("updatedAt")
                .setValue(System.currentTimeMillis());
    }

    private void syncProfileToCloudIfLoggedIn() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("weight", prefs.getString(PREF_WEIGHT, ""));
        profileMap.put("gender", prefs.getString(PREF_GENDER, "other"));
        profileMap.put("height", prefs.getString(PREF_HEIGHT, ""));
        profileMap.put("age", prefs.getString(PREF_AGE, ""));
        profileMap.put("activity", prefs.getInt(PREF_ACTIVITY, 0));
        profileMap.put("climate", prefs.getInt(PREF_CLIMATE, 0));
        profileMap.put("dailyGoal", getDailyGoal(prefs));
        profileMap.put("recommendedMin", prefs.getInt(PREF_RECOMMENDED_MIN, 1500));
        profileMap.put("recommendedMax", prefs.getInt(PREF_RECOMMENDED_MAX, 2000));
        profileMap.put("recommendedTarget", prefs.getInt(PREF_RECOMMENDED_WATER, 2500));
        profileMap.put("darkMode", prefs.getBoolean(PREF_DARK_MODE, false));
        profileMap.put("language", prefs.getString(PREF_LANGUAGE, SUPPORTED_LANGUAGES[0]));
        profileMap.put("alarmSound", prefs.getString(ReminderScheduler.PREF_ALARM_SOUND, ALARM_SOUND_VALUES[0]));
        profileMap.put("notificationSound", FIXED_NOTIFICATION_SOUND);
        profileMap.put("updatedAt", System.currentTimeMillis());

        databaseRoot.child("users")
                .child(user.getUid())
                .child("profile")
                .updateChildren(profileMap);
    }

    private void setupHomeScreen(View homeView) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int goalMl = getDailyGoal(prefs);
        int intakeMl = getTodayIntake(prefs);

        View quick200 = homeView.findViewById(R.id.quick_log_200);
        View quick350 = homeView.findViewById(R.id.quick_log_350);
        View quick500 = homeView.findViewById(R.id.quick_log_500);
        View logWaterButton = homeView.findViewById(R.id.btn_log_water);
        View setGoalButton = homeView.findViewById(R.id.btn_set_goal);
        View adjustProfileButton = homeView.findViewById(R.id.btn_adjust_profile);

        if (quick200 == null || quick350 == null || quick500 == null
                || logWaterButton == null || setGoalButton == null || adjustProfileButton == null) {
            Toast.makeText(this, "Thiếu thành phần giao diện trang hôm nay.", Toast.LENGTH_SHORT).show();
            return;
        }

        updateHomeProgress(homeView, intakeMl, goalMl);

        quick200.setOnClickListener(v -> addWaterIntake(200, homeView));
        quick350.setOnClickListener(v -> addWaterIntake(350, homeView));
        quick500.setOnClickListener(v -> addWaterIntake(500, homeView));

        logWaterButton.setOnClickListener(v -> showLogWaterDialog(homeView));
        setGoalButton.setOnClickListener(v -> showSetGoalDialog(homeView));

        adjustProfileButton.setOnClickListener(v -> showProfileOnboardingDialog(false));
    }

    private void showLogWaterDialog(View homeView) {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_log_water);
        View root = dialog.findViewById(android.R.id.content);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int todayIntake = getTodayIntake(prefs);
        int goal = getDailyGoal(prefs);

        View closeButton = root.findViewById(R.id.btn_close_log_water);
        View choice200 = root.findViewById(R.id.choice_200);
        View choice300 = root.findViewById(R.id.choice_300);
        View choice350 = root.findViewById(R.id.choice_350);
        View choice500 = root.findViewById(R.id.choice_500);
        EditText customInput = root.findViewById(R.id.et_custom_ml);
        TextView alreadyDrankView = root.findViewById(R.id.tv_already_drank_today);
        TextView totalView = root.findViewById(R.id.tv_dialog_total);
        TextView goalView = root.findViewById(R.id.tv_dialog_goal);
        TextView percentView = root.findViewById(R.id.tv_dialog_percent);
        android.widget.ProgressBar progressBar = root.findViewById(R.id.progress_dialog_total);
        View confirmButton = root.findViewById(R.id.btn_confirm_log);

        if (alreadyDrankView != null) {
            alreadyDrankView.setText(getString(R.string.log_already_drank_today, todayIntake));
        }

        final int[] selectedAmount = {500};
        setAmountChoiceSelection(new View[]{choice200, choice300, choice350, choice500}, 3);
        updateDialogIntakeSummary(todayIntake + selectedAmount[0], goal, totalView, goalView, percentView, progressBar);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        choice200.setOnClickListener(v -> {
            selectedAmount[0] = 200;
            customInput.setText("");
            setAmountChoiceSelection(new View[]{choice200, choice300, choice350, choice500}, 0);
            updateDialogIntakeSummary(todayIntake + selectedAmount[0], goal, totalView, goalView, percentView, progressBar);
        });
        choice300.setOnClickListener(v -> {
            selectedAmount[0] = 300;
            customInput.setText("");
            setAmountChoiceSelection(new View[]{choice200, choice300, choice350, choice500}, 1);
            updateDialogIntakeSummary(todayIntake + selectedAmount[0], goal, totalView, goalView, percentView, progressBar);
        });
        choice350.setOnClickListener(v -> {
            selectedAmount[0] = 350;
            customInput.setText("");
            setAmountChoiceSelection(new View[]{choice200, choice300, choice350, choice500}, 2);
            updateDialogIntakeSummary(todayIntake + selectedAmount[0], goal, totalView, goalView, percentView, progressBar);
        });
        choice500.setOnClickListener(v -> {
            selectedAmount[0] = 500;
            customInput.setText("");
            setAmountChoiceSelection(new View[]{choice200, choice300, choice350, choice500}, 3);
            updateDialogIntakeSummary(todayIntake + selectedAmount[0], goal, totalView, goalView, percentView, progressBar);
        });

        customInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Integer customAmount = parsePositiveInt(s.toString().trim());
                if (customAmount == null) {
                    updateDialogIntakeSummary(todayIntake + selectedAmount[0], goal, totalView, goalView, percentView, progressBar);
                    return;
                }
                setAmountChoiceSelection(new View[]{choice200, choice300, choice350, choice500}, -1);
                updateDialogIntakeSummary(todayIntake + customAmount, goal, totalView, goalView, percentView, progressBar);
            }
        });

        confirmButton.setOnClickListener(v -> {
            Integer customAmount = parsePositiveInt(customInput.getText().toString().trim());
            int amount = customAmount != null ? customAmount : selectedAmount[0];
            addWaterIntake(amount, homeView);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setAmountChoiceSelection(View[] choices, int selectedIndex) {
        for (int i = 0; i < choices.length; i++) {
            choices[i].setBackgroundResource(i == selectedIndex
                    ? R.drawable.bg_amount_option_selected
                    : R.drawable.bg_amount_option);
        }
    }

    private void updateDialogIntakeSummary(int projectedTotal,
                                           int goal,
                                           TextView totalView,
                                           TextView goalView,
                                           TextView percentView,
                                           android.widget.ProgressBar progressBar) {
        int percent = goal <= 0 ? 0 : Math.min(100, (int) Math.round((projectedTotal * 100.0) / goal));
        totalView.setText(String.valueOf(projectedTotal));
        goalView.setText(getString(R.string.target_water_dynamic, goal));
        percentView.setText(percent + "%");
        progressBar.setProgress(percent);
    }

    private void setupStatsScreen(View statsView) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int goalMl = getDailyGoal(prefs);
        View viewMoreHistory = statsView.findViewById(R.id.btn_view_more_history);
        if (viewMoreHistory != null) {
            viewMoreHistory.setOnClickListener(v -> showHydrationHistoryDialog(prefs));
        }

        int[] intakeLast7Days = new int[7];
        int[] weekdayLabelsLast7Days = new int[7];
        Calendar calendar = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) calendar.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            String dayKey = buildDateKey(day);
            int chartIndex = 6 - i;
            intakeLast7Days[chartIndex] = prefs.getInt(PREF_INTAKE_PREFIX + dayKey, 0);
            weekdayLabelsLast7Days[chartIndex] = getWeekdayLabelRes(day.get(Calendar.DAY_OF_WEEK));
        }

        int[] barIds = {
                R.id.bar_day_1,
                R.id.bar_day_2,
                R.id.bar_day_3,
                R.id.bar_day_4,
                R.id.bar_day_5,
                R.id.bar_day_6,
                R.id.bar_day_7
        };
        int[] dayLabelViewIds = {
            R.id.tv_day_label_1,
            R.id.tv_day_label_2,
            R.id.tv_day_label_3,
            R.id.tv_day_label_4,
            R.id.tv_day_label_5,
            R.id.tv_day_label_6,
            R.id.tv_day_label_7
        };

        TextView chartSelectedValue = statsView.findViewById(R.id.tv_chart_selected_value);
        FrameLayout chartContainer = statsView.findViewById(R.id.chart_bar_container) != null ?
            (FrameLayout) ((View) statsView.findViewById(R.id.chart_bar_container)).getParent() : null;

        if (chartSelectedValue != null) {
            chartSelectedValue.setVisibility(View.GONE);
        }

        int totalMl = 0;
        for (int i = 0; i < barIds.length; i++) {
            totalMl += intakeLast7Days[i];

            TextView dayLabel = statsView.findViewById(dayLabelViewIds[i]);
            if (dayLabel != null) {
                dayLabel.setText(weekdayLabelsLast7Days[i]);
                int labelColor = i == dayLabelViewIds.length - 1
                        ? R.color.primary_blue
                        : R.color.text_secondary;
                dayLabel.setTextColor(ContextCompat.getColor(this, labelColor));
            }

            View bar = statsView.findViewById(barIds[i]);
            if (bar == null) {
                continue;
            }
            int barHeight = calculateBarHeightPx(intakeLast7Days[i], goalMl);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
            params.height = barHeight;
            bar.setLayoutParams(params);

            String dayIntakeText = getString(
                    R.string.stats_day_intake_tooltip,
                    getString(weekdayLabelsLast7Days[i]),
                    intakeLast7Days[i]
            );
            ViewCompat.setTooltipText(bar, dayIntakeText);
            bar.setOnClickListener(v -> {
                if (chartSelectedValue != null && chartContainer != null) {
                    chartSelectedValue.setText(dayIntakeText);
                    chartSelectedValue.setVisibility(View.VISIBLE);
                    chartSelectedValue.post(() -> {
                        int[] barLocation = new int[2];
                        bar.getLocationOnScreen(barLocation);
                        int[] containerLocation = new int[2];
                        chartContainer.getLocationOnScreen(containerLocation);

                        float x = barLocation[0] - containerLocation[0]
                                + bar.getWidth() / 2f
                                - chartSelectedValue.getWidth() / 2f;
                        float maxX = Math.max(0f, chartContainer.getWidth() - chartSelectedValue.getWidth());
                        x = Math.max(0f, Math.min(x, maxX));

                        float y = bar.getY() - chartSelectedValue.getHeight() - (8f * getResources().getDisplayMetrics().density);
                        y = Math.max(0f, y);

                        chartSelectedValue.setX(x);
                        chartSelectedValue.setY(y);
                    });
                }
            });
        }
        // Khi bấm ra ngoài cột, ẩn label
        if (chartContainer != null && chartSelectedValue != null) {
            chartContainer.setOnClickListener(v -> chartSelectedValue.setVisibility(View.GONE));
        }

        int averageMl = totalMl / 7;
        double totalLiters = totalMl / 1000.0;

        TextView weeklyAverage = statsView.findViewById(R.id.tv_weekly_average_value);
        TextView totalWater = statsView.findViewById(R.id.tv_total_water_value);
        weeklyAverage.setText(getString(R.string.weekly_average_value_dynamic, averageMl));
        totalWater.setText(getString(R.string.total_water_value_dynamic, totalLiters));

        bindRecentLogs(statsView, prefs);
        bindAchievementProgress(statsView, prefs, goalMl);
    }

    private int getWeekdayLabelRes(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY:
                return R.string.weekday_mon;
            case Calendar.TUESDAY:
                return R.string.weekday_tue;
            case Calendar.WEDNESDAY:
                return R.string.weekday_wed;
            case Calendar.THURSDAY:
                return R.string.weekday_thu;
            case Calendar.FRIDAY:
                return R.string.weekday_fri;
            case Calendar.SATURDAY:
                return R.string.weekday_sat;
            case Calendar.SUNDAY:
            default:
                return R.string.weekday_sun;
        }
    }

    private void bindAchievementProgress(View statsView, SharedPreferences prefs, int goalMl) {
        android.widget.ProgressBar streakProgressBar = statsView.findViewById(R.id.pb_achievement_streak);
        android.widget.ProgressBar heroProgressBar = statsView.findViewById(R.id.pb_achievement_hero);
        TextView streakProgressText = statsView.findViewById(R.id.tv_achievement_streak_progress);
        TextView heroProgressText = statsView.findViewById(R.id.tv_achievement_hero_progress);

        int streakDays = getCurrentStreakDays(prefs);
        int streakPercent = Math.min(100, (int) Math.round((streakDays * 100.0) / 3.0));
        boolean streakUnlocked = streakDays >= 3;

        int todayIntake = getTodayIntake(prefs);
        int heroPercent;
        if (hasEverReachedDailyGoal(prefs, goalMl)) {
            heroPercent = 100;
        } else if (goalMl > 0) {
            heroPercent = Math.min(100, (int) Math.round((todayIntake * 100.0) / goalMl));
        } else {
            heroPercent = 0;
        }
        boolean heroUnlocked = heroPercent >= 100;

        streakProgressBar.setProgress(streakPercent);
        heroProgressBar.setProgress(heroPercent);
        streakProgressText.setText(streakUnlocked
                ? getString(R.string.achievement_unlocked)
                : getString(R.string.achievement_progress_dynamic, streakPercent));
        heroProgressText.setText(heroUnlocked
                ? getString(R.string.achievement_unlocked)
                : getString(R.string.achievement_progress_dynamic, heroPercent));
    }

    private boolean hasEverReachedDailyGoal(SharedPreferences prefs, int goalMl) {
        if (goalMl <= 0) {
            return false;
        }

        Calendar day = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            int intake = prefs.getInt(PREF_INTAKE_PREFIX + buildDateKey(day), 0);
            if (intake >= goalMl) {
                return true;
            }
            day.add(Calendar.DAY_OF_YEAR, -1);
        }
        return false;
    }

    private void setupExploreScreen(View exploreView) {
        TextView tabArticles = exploreView.findViewById(R.id.tab_articles);
        TextView tabTools = exploreView.findViewById(R.id.tab_tools);
        View sectionArticles = exploreView.findViewById(R.id.section_articles);
        View sectionTools = exploreView.findViewById(R.id.section_tools);
        LinearLayout toolsContainer = exploreView.findViewById(R.id.tools_list_container);

        bindExploreTools(toolsContainer);
        switchExploreTab(tabArticles, tabTools, sectionArticles, sectionTools, true);
        tabArticles.setOnClickListener(v -> switchExploreTab(tabArticles, tabTools, sectionArticles, sectionTools, true));
        tabTools.setOnClickListener(v -> switchExploreTab(tabArticles, tabTools, sectionArticles, sectionTools, false));

        bindExploreCards(exploreView, getFallbackExploreArticles());
        fetchExploreArticlesAsync(exploreView);
    }

    private void switchExploreTab(
            TextView tabArticles,
            TextView tabTools,
            View sectionArticles,
            View sectionTools,
            boolean showArticles
    ) {
        sectionArticles.setVisibility(showArticles ? View.VISIBLE : View.GONE);
        sectionTools.setVisibility(showArticles ? View.GONE : View.VISIBLE);

        tabArticles.setBackgroundResource(showArticles ? R.drawable.bg_chip_selected : R.drawable.bg_card);
        tabTools.setBackgroundResource(showArticles ? R.drawable.bg_card : R.drawable.bg_chip_selected);

        int activeTextColor = ContextCompat.getColor(this, R.color.white);
        int inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary);
        tabArticles.setTextColor(showArticles ? activeTextColor : inactiveTextColor);
        tabTools.setTextColor(showArticles ? inactiveTextColor : activeTextColor);
    }

    private void bindExploreTools(LinearLayout container) {
        if (container == null) {
            return;
        }

        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ExploreTool tool : getExploreTools()) {
            View item = inflater.inflate(R.layout.item_health_tool, container, false);
            TextView titleView = item.findViewById(R.id.tv_tool_title);
            TextView descView = item.findViewById(R.id.tv_tool_desc);
            TextView moreView = item.findViewById(R.id.btn_tool_more);

            titleView.setText(tool.title);
            descView.setText(tool.description);
            View.OnClickListener openTool = v -> showToolCalculatorDialog(tool);
            item.setOnClickListener(openTool);
            moreView.setOnClickListener(openTool);
            container.addView(item);
        }
    }

    private List<ExploreTool> getExploreTools() {
        List<ExploreTool> tools = new ArrayList<>();
        tools.add(new ExploreTool(
                TOOL_BODY_FAT,
                getString(R.string.tool_body_fat_title),
                getString(R.string.tool_body_fat_desc)
        ));
        tools.add(new ExploreTool(
                TOOL_BMI,
                getString(R.string.tool_bmi_title),
                getString(R.string.tool_bmi_desc)
        ));
        tools.add(new ExploreTool(
                TOOL_BMR,
                getString(R.string.tool_bmr_title),
                getString(R.string.tool_bmr_desc)
        ));
        return tools;
    }

    private void showToolCalculatorDialog(ExploreTool tool) {
        switch (tool.id) {
            case TOOL_BODY_FAT:
                showBodyFatCalculator(tool.title);
                return;
            case TOOL_BMI:
                showBmiCalculator(tool.title);
                return;
            case TOOL_BMR:
                showBmrCalculator(tool.title);
                return;
            case TOOL_TDEE:
                showTdeeCalculator(tool.title);
                return;
            case TOOL_DAILY_CALORIES:
                showDailyCaloriesCalculator(tool.title);
                return;
            case TOOL_CALORIES_BURN:
                showCaloriesBurnCalculator(tool.title);
                return;
            case TOOL_HEALTHY_WEIGHT:
                showHealthyWeightCalculator(tool.title);
                return;
            default:
                Toast.makeText(this, R.string.invalid_tool_input, Toast.LENGTH_SHORT).show();
        }
    }

    private void showBmiCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText weightInput = addNumberInput(root, getString(R.string.input_weight));
        EditText heightInput = addNumberInput(root, getString(R.string.input_height));
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double weight = requireDouble(weightInput);
            double heightCm = requireDouble(heightInput);
            double heightM = heightCm / 100.0;
            double bmi = weight / (heightM * heightM);
            resultView.setText(getString(R.string.result_bmi, bmi, classifyBmi(bmi)));
        });
    }

    private void showBodyFatCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText weightInput = addNumberInput(root, getString(R.string.input_weight));
        EditText heightInput = addNumberInput(root, getString(R.string.input_height));
        EditText ageInput = addNumberInput(root, getString(R.string.input_age));
        Spinner genderSpinner = addSpinner(root, R.array.gender_labels);
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double weight = requireDouble(weightInput);
            double heightCm = requireDouble(heightInput);
            int age = requireInt(ageInput);
            double heightM = heightCm / 100.0;
            double bmi = weight / (heightM * heightM);
            int sexFactor = genderSpinner.getSelectedItemPosition() == 0 ? 1 : 0;
            double bodyFat = 1.2 * bmi + 0.23 * age - 10.8 * sexFactor - 5.4;
            resultView.setText(getString(R.string.result_body_fat, bodyFat));
        });
    }

    private void showBmrCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText weightInput = addNumberInput(root, getString(R.string.input_weight));
        EditText heightInput = addNumberInput(root, getString(R.string.input_height));
        EditText ageInput = addNumberInput(root, getString(R.string.input_age));
        Spinner genderSpinner = addSpinner(root, R.array.gender_labels);
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double bmr = calculateBmr(
                    requireDouble(weightInput),
                    requireDouble(heightInput),
                    requireInt(ageInput),
                    genderSpinner.getSelectedItemPosition() == 0
            );
            resultView.setText(getString(R.string.result_bmr, bmr));
        });
    }

    private void showTdeeCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText weightInput = addNumberInput(root, getString(R.string.input_weight));
        EditText heightInput = addNumberInput(root, getString(R.string.input_height));
        EditText ageInput = addNumberInput(root, getString(R.string.input_age));
        Spinner genderSpinner = addSpinner(root, R.array.gender_labels);
        Spinner activitySpinner = addSpinner(root, R.array.tdee_activity_labels);
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double bmr = calculateBmr(
                    requireDouble(weightInput),
                    requireDouble(heightInput),
                    requireInt(ageInput),
                    genderSpinner.getSelectedItemPosition() == 0
            );
            double[] factors = {1.2, 1.375, 1.55, 1.725, 1.9};
            int index = Math.max(0, Math.min(factors.length - 1, activitySpinner.getSelectedItemPosition()));
            resultView.setText(getString(R.string.result_tdee, bmr * factors[index]));
        });
    }

    private void showDailyCaloriesCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText weightInput = addNumberInput(root, getString(R.string.input_weight));
        EditText heightInput = addNumberInput(root, getString(R.string.input_height));
        EditText ageInput = addNumberInput(root, getString(R.string.input_age));
        Spinner genderSpinner = addSpinner(root, R.array.gender_labels);
        Spinner activitySpinner = addSpinner(root, R.array.tdee_activity_labels);
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double bmr = calculateBmr(
                    requireDouble(weightInput),
                    requireDouble(heightInput),
                    requireInt(ageInput),
                    genderSpinner.getSelectedItemPosition() == 0
            );
            double[] factors = {1.2, 1.375, 1.55, 1.725, 1.9};
            int index = Math.max(0, Math.min(factors.length - 1, activitySpinner.getSelectedItemPosition()));
            double maintain = bmr * factors[index];
            resultView.setText(getString(R.string.result_daily_calories, maintain, maintain - 500, maintain + 300));
        });
    }

    private void showCaloriesBurnCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText weightInput = addNumberInput(root, getString(R.string.input_weight));
        EditText durationInput = addNumberInput(root, getString(R.string.input_duration_minutes));
        Spinner workoutSpinner = addSpinner(root, R.array.workout_labels);
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double[] mets = {3.8, 7.5, 8.5, 12.0};
            int index = Math.max(0, Math.min(mets.length - 1, workoutSpinner.getSelectedItemPosition()));
            double burned = mets[index] * requireDouble(weightInput) * (requireDouble(durationInput) / 60.0);
            resultView.setText(getString(R.string.result_calories_burn, burned));
        });
    }

    private void showHealthyWeightCalculator(String title) {
        LinearLayout root = buildCalculatorRoot();
        EditText heightInput = addNumberInput(root, getString(R.string.input_height));
        TextView resultView = addResultView(root);

        showCalculatorDialog(title, root, resultView, () -> {
            double heightM = requireDouble(heightInput) / 100.0;
            resultView.setText(getString(R.string.result_healthy_weight, 18.5 * heightM * heightM, 24.9 * heightM * heightM));
        });
    }

    private LinearLayout buildCalculatorRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(10);
        root.setPadding(padding, padding, padding, padding);
        return root;
    }

    private EditText addNumberInput(LinearLayout root, String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private Spinner addSpinner(LinearLayout root, int arrayResId) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                arrayResId,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        root.addView(spinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return spinner;
    }

    private TextView addResultView(LinearLayout root) {
        TextView resultView = new TextView(this);
        resultView.setText(R.string.result_placeholder);
        resultView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        resultView.setPadding(0, dpToPx(10), 0, 0);
        root.addView(resultView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return resultView;
    }

    private void showCalculatorDialog(String title, View content, TextView resultView, Runnable calculateAction) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.calculate, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                try {
                    calculateAction.run();
                } catch (IllegalArgumentException ex) {
                    Toast.makeText(this, R.string.invalid_tool_input, Toast.LENGTH_SHORT).show();
                    resultView.setText(R.string.result_placeholder);
                }
            });
        });
        dialog.show();
    }

    private double requireDouble(EditText input) {
        String raw = input.getText() == null ? "" : input.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            throw new IllegalArgumentException("empty");
        }
        try {
            double value = Double.parseDouble(raw);
            if (value <= 0) {
                throw new IllegalArgumentException("invalid");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid");
        }
    }

    private int requireInt(EditText input) {
        return (int) Math.round(requireDouble(input));
    }

    private double calculateBmr(double weight, double heightCm, int age, boolean male) {
        return male
                ? (10 * weight) + (6.25 * heightCm) - (5 * age) + 5
                : (10 * weight) + (6.25 * heightCm) - (5 * age) - 161;
    }

    private String classifyBmi(double bmi) {
        if (bmi < 18.5) {
            return getString(R.string.bmi_underweight);
        }
        if (bmi < 25.0) {
            return getString(R.string.bmi_normal);
        }
        if (bmi < 30.0) {
            return getString(R.string.bmi_overweight);
        }
        return getString(R.string.bmi_obese);
    }

    private int dpToPx(int dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

    private void playPreviewSound(String soundValue) {
        stopPreviewSound();
        previewPlayer = MediaPlayer.create(this, mapSoundToRes(soundValue));
        if (previewPlayer == null) {
            return;
        }
        previewPlayer.setOnCompletionListener(mp -> {
            mp.release();
            if (previewPlayer == mp) {
                previewPlayer = null;
            }
        });
        previewPlayer.start();
    }

    private void stopPreviewSound() {
        if (previewPlayer == null) {
            return;
        }
        if (previewPlayer.isPlaying()) {
            previewPlayer.stop();
        }
        previewPlayer.release();
        previewPlayer = null;
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

    private void bindExploreCards(View exploreView, List<ExploreArticle> articles) {
        if (articles == null || articles.size() < 3) {
            return;
        }

        int[] cardIds = {R.id.article_card_1, R.id.article_card_2, R.id.article_card_3};
        int[] titleIds = {R.id.tv_article_title_1, R.id.tv_article_title_2, R.id.tv_article_title_3};
        int[] summaryIds = {R.id.tv_article_summary_1, R.id.tv_article_summary_2, R.id.tv_article_summary_3};
        int[] readTimeIds = {R.id.tv_article_read_time_1, R.id.tv_article_read_time_2, R.id.tv_article_read_time_3};

        for (int i = 0; i < 3; i++) {
            ExploreArticle article = articles.get(i);
            TextView titleView = exploreView.findViewById(titleIds[i]);
            TextView summaryView = exploreView.findViewById(summaryIds[i]);
            TextView readTimeView = exploreView.findViewById(readTimeIds[i]);
            View cardView = exploreView.findViewById(cardIds[i]);

            titleView.setText(article.title);
            summaryView.setText(article.summary);
            readTimeView.setText(article.readTime);
            cardView.setOnClickListener(v -> openArticleInWebView(article.title, article.url));
        }
    }

    private List<ExploreArticle> getFallbackExploreArticles() {
        List<ExploreArticle> fallback = new ArrayList<>();
        fallback.add(new ExploreArticle(
                getString(R.string.article_title_1),
                getString(R.string.article_summary_1),
                getString(R.string.article_url_1),
                getString(R.string.article_read_time_1)
        ));
        fallback.add(new ExploreArticle(
                getString(R.string.article_title_2),
                getString(R.string.article_summary_2),
                getString(R.string.article_url_2),
                getString(R.string.article_read_time_2)
        ));
        fallback.add(new ExploreArticle(
                getString(R.string.article_title_3),
                getString(R.string.article_summary_3),
                getString(R.string.article_url_3),
                getString(R.string.article_read_time_3)
        ));
        return fallback;
    }

    private void fetchExploreArticlesAsync(View exploreView) {
        ioExecutor.execute(() -> {
            List<ExploreArticle> apiArticles = fetchExploreArticlesFromApi();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (apiArticles.size() >= 3) {
                    bindExploreCards(exploreView, apiArticles);
                }
            });
        });
    }

    private List<ExploreArticle> fetchExploreArticlesFromApi() {
        List<ExploreArticle> result = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(EXPLORE_RSS_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AppNhacNhoUongNuoc/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return result;
            }

            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder xmlBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    xmlBuilder.append(line);
                }
                parseRssArticles(xmlBuilder.toString(), result);
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    private void parseRssArticles(String xmlContent, List<ExploreArticle> output) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xmlContent));

        boolean inItem = false;
        String title = null;
        String link = null;
        String description = null;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT && output.size() < 3) {
            String tag = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("item".equals(tag)) {
                    inItem = true;
                    title = null;
                    link = null;
                    description = null;
                } else if (inItem && "title".equals(tag)) {
                    title = parser.nextText();
                } else if (inItem && "link".equals(tag)) {
                    link = parser.nextText();
                } else if (inItem && "description".equals(tag)) {
                    description = parser.nextText();
                }
            } else if (eventType == XmlPullParser.END_TAG && "item".equals(tag)) {
                inItem = false;
                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(link)) {
                    String summary = cleanupHtml(description);
                    if (TextUtils.isEmpty(summary)) {
                        summary = title;
                    }
                    output.add(new ExploreArticle(
                            title.trim(),
                            summary,
                            link.trim(),
                            estimateReadTime(summary)
                    ));
                }
            }
            eventType = parser.next();
        }
    }

    private String cleanupHtml(String source) {
        if (TextUtils.isEmpty(source)) {
            return "";
        }
        String noTags = source.replaceAll("<[^>]*>", " ");
        String noEntities = noTags.replaceAll("&[^;]+;", " ");
        return noEntities.replaceAll("\\s+", " ").trim();
    }

    private String estimateReadTime(String text) {
        if (TextUtils.isEmpty(text)) {
            return "1 min";
        }
        String[] words = text.trim().split("\\s+");
        int minutes = Math.max(1, (int) Math.ceil(words.length / 180.0));
        return minutes + " min";
    }

    private void openArticleInWebView(String title, String url) {
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.invalid_article_url, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ArticleWebViewActivity.class);
        intent.putExtra(ArticleWebViewActivity.EXTRA_TITLE, title);
        intent.putExtra(ArticleWebViewActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    private void bindRecentLogs(View statsView, SharedPreferences prefs) {
        TextView time1 = statsView.findViewById(R.id.time1);
        TextView amount1 = statsView.findViewById(R.id.tv_amount_1);
        TextView type1 = statsView.findViewById(R.id.tv_type_1);
        TextView time2 = statsView.findViewById(R.id.time2);
        TextView amount2 = statsView.findViewById(R.id.tv_amount_2);
        TextView type2 = statsView.findViewById(R.id.tv_type_2);

        List<HydrationLogEntry> todayLogs = getTodayLogs(prefs, 2);
        bindSingleLog(time1, amount1, type1, todayLogs.size() > 0 ? todayLogs.get(0) : null);
        bindSingleLog(time2, amount2, type2, todayLogs.size() > 1 ? todayLogs.get(1) : null);
    }

    private void showHydrationHistoryDialog(SharedPreferences prefs) {
        List<HydrationLogEntry> allLogs = getAllLogs(prefs, 50);
        if (allLogs.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.history_dialog_title)
                    .setMessage(R.string.history_dialog_empty)
                    .setPositiveButton(R.string.close, null)
                    .show();
            return;
        }

        String[] lines = new String[allLogs.size()];
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        for (int i = 0; i < allLogs.size(); i++) {
            HydrationLogEntry entry = allLogs.get(i);
            String when = dateFormat.format(new Date(entry.timestamp));
            lines[i] = getString(R.string.history_dialog_item, when, entry.amountMl);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.history_dialog_title)
                .setItems(lines, null)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void bindSingleLog(TextView timeView, TextView amountView, TextView typeView, HydrationLogEntry entry) {
        if (entry == null) {
            timeView.setText(R.string.no_log_yet);
            amountView.setText(getString(R.string.log_amount_dynamic, 0));
            typeView.setText(R.string.no_log_yet);
            return;
        }

        String clock = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(entry.timestamp));
        timeView.setText(getString(R.string.today_time_format, clock));
        amountView.setText(getString(R.string.log_amount_dynamic, entry.amountMl));
        typeView.setText(R.string.water_type_plain);
    }

    private void updateHomeProgress(View homeView, int intakeMl, int goalMl) {
        TextView currentWater = homeView.findViewById(R.id.tvCurrentWater);
        TextView targetWater = homeView.findViewById(R.id.tvTargetWater);
        TextView percentage = homeView.findViewById(R.id.tvPercentage);
        TextView todayTitle = homeView.findViewById(R.id.tv_today_progress_title);
        TextView progressSummary = homeView.findViewById(R.id.tv_progress_summary);
        TextView progressCheer = homeView.findViewById(R.id.tv_progress_cheer);
        android.widget.ProgressBar circular = homeView.findViewById(R.id.circularProgressBar);
        android.widget.ProgressBar horizontal = homeView.findViewById(R.id.horizontalProgressBar);

        int percent = goalMl <= 0 ? 0 : Math.min(100, (int) Math.round((intakeMl * 100.0) / goalMl));

        currentWater.setText(String.valueOf(intakeMl));
        targetWater.setText(getString(R.string.target_water_dynamic, goalMl));
        percentage.setText(percent + "%");
        circular.setProgress(percent);
        horizontal.setProgress(percent);

        if (todayTitle != null) {
            if (percent >= 100) {
                todayTitle.setText(R.string.goal_reached_title);
            } else {
                int remainMl = Math.max(0, goalMl - intakeMl);
                todayTitle.setText(getString(R.string.home_remaining_hint_dynamic, remainMl));
            }
        }

        if (progressSummary != null) {
            progressSummary.setText(getString(
                    R.string.home_progress_summary_dynamic,
                    formatMlAsLiterText(intakeMl),
                    formatMlAsLiterText(goalMl)
            ));
        }

        if (progressCheer != null) {
            if (percent >= 100) {
                progressCheer.setText(R.string.goal_reached_title);
            } else if (percent >= 60) {
                progressCheer.setText(R.string.home_progress_cheer_strong);
            } else {
                progressCheer.setText(R.string.home_progress_cheer);
            }
        }
    }

    private String formatMlAsLiterText(int mlValue) {
        if (mlValue <= 0) {
            return "0 lít";
        }
        String liters = String.format(Locale.US, "%.1f", mlValue / 1000.0);
        liters = Pattern.compile("\\.0$").matcher(liters).replaceFirst("");
        return liters + " lít";
    }

    private void addWaterIntake(int amountMl, View homeView) {
        if (amountMl <= 0) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String todayKey = getTodayKey();
        String intakePrefKey = PREF_INTAKE_PREFIX + todayKey;

        int currentIntake = prefs.getInt(intakePrefKey, 0);
        int updatedIntake = Math.min(10000, currentIntake + amountMl);
        prefs.edit().putInt(intakePrefKey, updatedIntake).apply();

        appendLogEntry(prefs, amountMl);
        syncLogsToCloudIfLoggedIn();

        int goal = getDailyGoal(prefs);
        boolean reachedGoalNow = goal > 0 && currentIntake < goal && updatedIntake >= goal;

        updateHomeProgress(homeView, updatedIntake, goal);
        Toast.makeText(this, getString(R.string.water_logged, amountMl), Toast.LENGTH_SHORT).show();
        syncTodayToCloudIfLoggedIn(updatedIntake, goal);

        Bundle params = new Bundle();
        params.putInt("amount_ml", amountMl);
        params.putInt("total_today_ml", updatedIntake);
        params.putInt("goal_ml", goal);
        firebaseAnalytics.logEvent("water_logged", params);

        if (reachedGoalNow) {
            showGoalReachedDialog(goal);
        }
    }

    private void showSetGoalDialog(View homeView) {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_set_goal);
        View root = dialog.findViewById(android.R.id.content);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentGoal = Math.max(500, Math.min(getDailyGoal(prefs), 4000));

        View closeButton = root.findViewById(R.id.btn_close_set_goal);
        TextView goalAmount = root.findViewById(R.id.tv_set_goal_amount);
        TextView manualValue = root.findViewById(R.id.tv_set_goal_manual_value);
        SeekBar seekBar = root.findViewById(R.id.seek_set_goal);
        View quick1500 = root.findViewById(R.id.quick_goal_1500);
        View quick2000 = root.findViewById(R.id.quick_goal_2000);
        View quick2500 = root.findViewById(R.id.quick_goal_2500);
        View saveButton = root.findViewById(R.id.btn_save_set_goal);

        View[] quickViews = new View[]{quick1500, quick2000, quick2500};
        int[] quickValues = new int[]{1500, 2000, 2500};
        final int[] selectedGoal = {currentGoal};

        seekBar.setProgress(currentGoal - 500);
        updateSetGoalPreview(goalAmount, manualValue, selectedGoal[0]);
        updateQuickGoalSelection(quickViews, quickValues, selectedGoal[0]);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int mapped = 500 + progress;
                int rounded = roundTo50(mapped);
                selectedGoal[0] = Math.max(500, Math.min(4000, rounded));
                updateSetGoalPreview(goalAmount, manualValue, selectedGoal[0]);
                updateQuickGoalSelection(quickViews, quickValues, selectedGoal[0]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(selectedGoal[0] - 500);
            }
        });

        quick1500.setOnClickListener(v -> {
            selectedGoal[0] = 1500;
            seekBar.setProgress(1000);
            updateSetGoalPreview(goalAmount, manualValue, selectedGoal[0]);
            updateQuickGoalSelection(quickViews, quickValues, selectedGoal[0]);
        });

        quick2000.setOnClickListener(v -> {
            selectedGoal[0] = 2000;
            seekBar.setProgress(1500);
            updateSetGoalPreview(goalAmount, manualValue, selectedGoal[0]);
            updateQuickGoalSelection(quickViews, quickValues, selectedGoal[0]);
        });

        quick2500.setOnClickListener(v -> {
            selectedGoal[0] = 2500;
            seekBar.setProgress(2000);
            updateSetGoalPreview(goalAmount, manualValue, selectedGoal[0]);
            updateQuickGoalSelection(quickViews, quickValues, selectedGoal[0]);
        });

        saveButton.setOnClickListener(v -> {
            prefs.edit().putInt(PREF_DAILY_GOAL, selectedGoal[0]).apply();
            int current = getTodayIntake(prefs);
            updateHomeProgress(homeView, current, selectedGoal[0]);
            Toast.makeText(this, getString(R.string.goal_updated, selectedGoal[0]), Toast.LENGTH_SHORT).show();
            syncTodayToCloudIfLoggedIn(current, selectedGoal[0]);

            Bundle params = new Bundle();
            params.putInt("goal_ml", selectedGoal[0]);
            params.putInt("current_intake_ml", current);
            firebaseAnalytics.logEvent("goal_updated", params);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateSetGoalPreview(TextView goalAmount, TextView manualValue, int valueMl) {
        goalAmount.setText(String.valueOf(valueMl));
        manualValue.setText(getString(R.string.log_amount_dynamic, valueMl));
    }

    private void updateQuickGoalSelection(View[] quickViews, int[] quickValues, int selectedGoal) {
        for (int i = 0; i < quickViews.length; i++) {
            boolean isSelected = quickValues[i] == selectedGoal;
            quickViews[i].setBackgroundResource(isSelected
                    ? R.drawable.bg_amount_option_selected
                    : R.drawable.bg_amount_option);
        }
    }

    private void showGoalReachedDialog(int goalMl) {
        Dialog dialog = createFullScreenDialog(R.layout.dialog_goal_reached);
        View root = dialog.findViewById(android.R.id.content);

        TextView message = root.findViewById(R.id.tv_goal_reached_message);
        View continueButton = root.findViewById(R.id.btn_goal_reached_continue);

        message.setText(getString(R.string.goal_reached_message_dynamic, goalMl));
        continueButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private int getDailyGoal(SharedPreferences prefs) {
        int recommended = prefs.getInt(PREF_RECOMMENDED_WATER, 2500);
        return prefs.getInt(PREF_DAILY_GOAL, recommended);
    }

    private int getTodayIntake(SharedPreferences prefs) {
        return prefs.getInt(PREF_INTAKE_PREFIX + getTodayKey(), 0);
    }

    private void appendLogEntry(SharedPreferences prefs, int amountMl) {
        JSONArray history = readHistoryArray(prefs);
        JSONObject entry = new JSONObject();
        try {
            entry.put("timestamp", System.currentTimeMillis());
            entry.put("amount", amountMl);
            history.put(entry);
        } catch (JSONException ignored) {
            return;
        }

        while (history.length() > 240) {
            history.remove(0);
        }

        prefs.edit().putString(PREF_LOG_HISTORY, history.toString()).apply();
    }

    private List<HydrationLogEntry> getTodayLogs(SharedPreferences prefs, int maxCount) {
        List<HydrationLogEntry> result = new ArrayList<>();
        JSONArray history = readHistoryArray(prefs);
        Calendar today = Calendar.getInstance();

        for (int i = history.length() - 1; i >= 0 && result.size() < maxCount; i--) {
            JSONObject obj = history.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            long timestamp = obj.optLong("timestamp", 0L);
            int amount = obj.optInt("amount", 0);
            if (timestamp <= 0 || amount <= 0) {
                continue;
            }

            Calendar logDay = Calendar.getInstance();
            logDay.setTimeInMillis(timestamp);
            if (isSameDay(today, logDay)) {
                result.add(new HydrationLogEntry(timestamp, amount));
            }
        }

        return result;
    }

    private List<HydrationLogEntry> getAllLogs(SharedPreferences prefs, int maxCount) {
        List<HydrationLogEntry> result = new ArrayList<>();
        JSONArray history = readHistoryArray(prefs);

        for (int i = history.length() - 1; i >= 0 && result.size() < maxCount; i--) {
            JSONObject obj = history.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            long timestamp = obj.optLong("timestamp", 0L);
            int amount = obj.optInt("amount", 0);
            if (timestamp <= 0 || amount <= 0) {
                continue;
            }
            result.add(new HydrationLogEntry(timestamp, amount));
        }

        return result;
    }

    private JSONArray readHistoryArray(SharedPreferences prefs) {
        String raw = prefs.getString(PREF_LOG_HISTORY, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException ex) {
            return new JSONArray();
        }
    }

    private int getCurrentStreakDays(SharedPreferences prefs) {
        int streak = 0;
        Calendar day = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            String key = PREF_INTAKE_PREFIX + buildDateKey(day);
            if (prefs.getInt(key, 0) <= 0) {
                break;
            }
            streak++;
            day.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private int calculateBarHeightPx(int intakeMl, int goalMl) {
        int minDp = 12;
        int maxDp = 120;
        int valueDp;
        if (goalMl <= 0 || intakeMl <= 0) {
            valueDp = minDp;
        } else {
            double ratio = Math.min(1.0, intakeMl / (double) goalMl);
            valueDp = minDp + (int) Math.round((maxDp - minDp) * ratio);
        }
        float density = getResources().getDisplayMetrics().density;
        return (int) (valueDp * density);
    }

    private void showAmountInputDialog(int titleRes, int hintRes, AmountConsumer consumer) {
        EditText input = new EditText(this);
        input.setHint(hintRes);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(40, 24, 40, 24);

        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(input)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Integer amount = parsePositiveInt(input.getText().toString().trim());
                    if (amount == null) {
                        Toast.makeText(this, R.string.input_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    consumer.onAmount(amount);
                })
                .show();
    }

    private String getTodayKey() {
        return buildDateKey(Calendar.getInstance());
    }

    private String buildDateKey(Calendar calendar) {
        return String.format(
                Locale.US,
                "%04d%02d%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private WaterRecommendation calculateRecommendedWater(double weightKg, int age, int activityLevel) {
        int minPerKg;
        int maxPerKg;

        if (activityLevel == 3 || (age >= 10 && age <= 18)) {
            minPerKg = 40;
            maxPerKg = 40;
        } else if (activityLevel == 2) {
            minPerKg = 40;
            maxPerKg = 50;
        } else if (activityLevel == 1) {
            minPerKg = 35;
            maxPerKg = 45;
        } else {
            minPerKg = 30;
            maxPerKg = 40;
        }

        int minMl = roundTo50((int) Math.round(weightKg * minPerKg));
        int maxMl = roundTo50((int) Math.round(weightKg * maxPerKg));
        int target = roundTo50((minMl + maxMl) / 2);

        minMl = Math.max(1200, Math.min(minMl, 6000));
        maxMl = Math.max(minMl, Math.min(maxMl, 7000));
        target = Math.max(minMl, Math.min(target, maxMl));
        return new WaterRecommendation(minMl, maxMl, target);
    }

    private int roundTo50(int value) {
        return ((int) Math.round(value / 50.0)) * 50;
    }

    private void maybeShowFirstLaunchOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_ONBOARDING_DONE, false)) {
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showProfileOnboardingDialog(true);
            return;
        }

        databaseRoot.child("users")
                .child(currentUser.getUid())
                .child("profile")
                .get()
                .addOnSuccessListener(profileSnapshot -> {
                    if (hasRemoteProfileData(profileSnapshot)) {
                        prefs.edit().putBoolean(PREF_ONBOARDING_DONE, true).apply();
                        syncCloudToLocalThenRefresh();
                    } else {
                        showProfileOnboardingDialog(true);
                    }
                })
                .addOnFailureListener(error -> {
                    if (hasLocalAccountData(prefs)) {
                        prefs.edit().putBoolean(PREF_ONBOARDING_DONE, true).apply();
                        return;
                    }
                    showProfileOnboardingDialog(true);
                });
    }

    private boolean hasRemoteProfileData(DataSnapshot profileSnapshot) {
        if (profileSnapshot == null || !profileSnapshot.exists() || !profileSnapshot.hasChildren()) {
            return false;
        }

        String weight = profileSnapshot.child("weight").getValue(String.class);
        String age = profileSnapshot.child("age").getValue(String.class);
        Integer dailyGoal = profileSnapshot.child("dailyGoal").getValue(Integer.class);
        Integer recommendedTarget = profileSnapshot.child("recommendedTarget").getValue(Integer.class);

        return !TextUtils.isEmpty(weight)
                || !TextUtils.isEmpty(age)
                || (dailyGoal != null && dailyGoal > 0)
                || (recommendedTarget != null && recommendedTarget > 0);
        }

    private void showProfileOnboardingDialog(boolean mandatory) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad / 2);

        TextView intro = new TextView(this);
        intro.setText(R.string.onboarding_intro);
        intro.setPadding(0, 0, 0, pad / 2);
        root.addView(intro);

        EditText weightInput = new EditText(this);
        weightInput.setHint(R.string.input_weight);
        weightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weightInput.setText(prefs.getString(PREF_WEIGHT, ""));
        root.addView(weightInput);

        EditText ageInput = new EditText(this);
        ageInput.setHint(R.string.input_age);
        ageInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        ageInput.setText(prefs.getString(PREF_AGE, ""));
        root.addView(ageInput);

        Spinner genderSpinner = new Spinner(this);
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_labels,
            android.R.layout.simple_spinner_item
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);
        String savedGender = prefs.getString(PREF_GENDER, "other");
        int genderIndex = "male".equals(savedGender) ? 0 : ("female".equals(savedGender) ? 1 : 2);
        int genderItemCount = genderAdapter.getCount();
        int safeGenderIndex = Math.max(0, Math.min(genderIndex, Math.max(0, genderItemCount - 1)));
        genderSpinner.setSelection(safeGenderIndex, false);
        root.addView(genderSpinner);

        Spinner activitySpinner = new Spinner(this);
        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.activity_level_labels,
                android.R.layout.simple_spinner_item
        );
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(activityAdapter);
        int savedActivityIndex = prefs.getInt(PREF_ACTIVITY, 0);
        int activityItemCount = activityAdapter.getCount();
        int safeActivityIndex = Math.max(0, Math.min(savedActivityIndex, Math.max(0, activityItemCount - 1)));
        activitySpinner.setSelection(safeActivityIndex, false);
        root.addView(activitySpinner);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.onboarding_title)
                .setView(root)
                .setCancelable(!mandatory)
                .setNegativeButton(mandatory ? null : getString(R.string.cancel), (d, w) -> d.dismiss())
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                Double weight = parsePositiveDouble(weightInput.getText().toString().trim());
                Integer age = parsePositiveInt(ageInput.getText().toString().trim());
                int activity = activitySpinner.getSelectedItemPosition();
                int selectedGenderPosition = genderSpinner.getSelectedItemPosition();
                String selectedGender = selectedGenderPosition == 0
                    ? "male"
                    : (selectedGenderPosition == 1 ? "female" : "other");

                if (weight == null || age == null) {
                    Toast.makeText(this, R.string.input_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                WaterRecommendation recommendation = calculateRecommendedWater(weight, age, activity);

                prefs.edit()
                        .putString(PREF_WEIGHT, weightInput.getText().toString().trim())
                        .putString(PREF_AGE, ageInput.getText().toString().trim())
                    .putString(PREF_GENDER, selectedGender)
                        .putInt(PREF_ACTIVITY, activity)
                        .putInt(PREF_RECOMMENDED_MIN, recommendation.minMl)
                        .putInt(PREF_RECOMMENDED_MAX, recommendation.maxMl)
                        .putInt(PREF_RECOMMENDED_WATER, recommendation.targetMl)
                        .putInt(PREF_DAILY_GOAL, recommendation.targetMl)
                        .putBoolean(PREF_ONBOARDING_DONE, true)
                        .apply();

                    syncProfileToCloudIfLoggedIn();

                Bundle params = new Bundle();
                params.putDouble("weight_kg", weight);
                params.putInt("age", age);
                params.putInt("activity_level", activity);
                params.putInt("target_ml", recommendation.targetMl);
                firebaseAnalytics.logEvent("onboarding_completed", params);

                if (container != null && container.getChildCount() > 0) {
                    showScreen(R.id.nav_today);
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private Double parsePositiveDouble(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parsePositiveInt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void setupReminderSettings(View settingsView, SharedPreferences prefs) {
        setupReminderList(settingsView);

        Spinner alarmSoundSpinner = settingsView.findViewById(R.id.spinner_alarm_sound);
        View previewAlarmButton = settingsView.findViewById(R.id.btn_preview_alarm_sound);
        View stopPreviewAlarmButton = settingsView.findViewById(R.id.btn_stop_preview_alarm_sound);
        ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.alarm_sound_labels,
                android.R.layout.simple_spinner_item
        );
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alarmSoundSpinner.setAdapter(soundAdapter);

        boolean fixedNotificationChanged = !FIXED_NOTIFICATION_SOUND.equals(
            prefs.getString(ReminderScheduler.PREF_NOTIFICATION_SOUND, FIXED_NOTIFICATION_SOUND)
        );
        if (fixedNotificationChanged) {
            prefs.edit().putString(ReminderScheduler.PREF_NOTIFICATION_SOUND, FIXED_NOTIFICATION_SOUND).apply();
            syncProfileToCloudIfLoggedIn();
        }

        String selectedSound = prefs.getString(ReminderScheduler.PREF_ALARM_SOUND, ALARM_SOUND_VALUES[0]);
        int selectedSoundIndex = findSoundIndex(selectedSound);
        alarmSoundSpinner.setSelection(selectedSoundIndex, false);
        alarmSoundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= ALARM_SOUND_VALUES.length) {
                    return;
                }
                String newValue = ALARM_SOUND_VALUES[position];
                if (!newValue.equals(prefs.getString(ReminderScheduler.PREF_ALARM_SOUND, ALARM_SOUND_VALUES[0]))) {
                    prefs.edit().putString(ReminderScheduler.PREF_ALARM_SOUND, newValue).apply();
                    syncProfileToCloudIfLoggedIn();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        previewAlarmButton.setOnClickListener(v ->
                playPreviewSound(ALARM_SOUND_VALUES[Math.max(0, Math.min(ALARM_SOUND_VALUES.length - 1,
                        alarmSoundSpinner.getSelectedItemPosition()))])
        );
        stopPreviewAlarmButton.setOnClickListener(v -> stopPreviewSound());
    }

    private void setupReminderList(View settingsView) {
        LinearLayout reminderListContainer = settingsView.findViewById(R.id.reminder_list_container);
        View addReminderButton = settingsView.findViewById(R.id.btn_add_reminder_slot);

        addReminderButton.setOnClickListener(v -> showReminderEditorDialog(null, reminderListContainer));
        renderReminderList(reminderListContainer);
    }

    private void renderReminderList(@NonNull LinearLayout container) {
        List<ReminderScheduler.ReminderItem> items = ReminderScheduler.getReminderItems(this);
        Collections.sort(items, (a, b) -> {
            int minuteA = a.hour * 60 + a.minute;
            int minuteB = b.hour * 60 + b.minute;
            if (minuteA != minuteB) {
                return Integer.compare(minuteA, minuteB);
            }
            return Integer.compare(a.id, b.id);
        });

        container.removeAllViews();
        if (items.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(R.string.no_reminder_slots);
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            emptyView.setTextSize(13f);
            container.addView(emptyView);
            return;
        }

        for (ReminderScheduler.ReminderItem item : items) {
            ReminderScheduler.ReminderItem localItem = cloneReminderItem(item);
            View itemView = getLayoutInflater().inflate(R.layout.item_reminder_slot, container, false);

            TextView timeView = itemView.findViewById(R.id.tv_reminder_time);
            TextView typeView = itemView.findViewById(R.id.tv_reminder_type);
            SwitchCompat enabledSwitch = itemView.findViewById(R.id.switch_reminder_enabled);
            LinearLayout repeatDaysLayout = itemView.findViewById(R.id.layout_repeat_days);
            View editButton = itemView.findViewById(R.id.btn_edit_reminder);
            View deleteButton = itemView.findViewById(R.id.btn_delete_reminder);

            timeView.setText(formatReminderTime(localItem.hour, localItem.minute));
            typeView.setText(formatReminderDetails(localItem));
            renderRepeatDayChips(repeatDaysLayout, localItem.repeatMask);

            enabledSwitch.setChecked(localItem.enabled);
            enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localItem.enabled = isChecked;
                ReminderScheduler.upsertReminder(this, localItem);
                syncRemindersToCloudIfLoggedIn();
            });

            editButton.setOnClickListener(v -> showReminderEditorDialog(localItem, container));
            deleteButton.setOnClickListener(v -> {
                ReminderScheduler.deleteReminder(this, localItem.id);
                syncRemindersToCloudIfLoggedIn();
                renderReminderList(container);
            });

            container.addView(itemView);
        }
    }

    private void renderRepeatDayChips(@NonNull LinearLayout container, int repeatMask) {
        int normalizedMask = ReminderScheduler.normalizeRepeatMask(repeatMask);
        container.removeAllViews();

        for (int i = 0; i < WEEKDAY_LABEL_IDS.length; i++) {
            boolean isSelected = (normalizedMask & (1 << i)) != 0;
            TextView chip = new TextView(this);
            chip.setText(getString(WEEKDAY_LABEL_IDS[i]));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextSize(11f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            chip.setBackgroundResource(isSelected
                    ? R.drawable.bg_day_chip_active
                    : R.drawable.bg_day_chip_inactive);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(34),
                    dpToPx(28)
            );
            if (i > 0) {
                params.leftMargin = dpToPx(6);
            }
            chip.setLayoutParams(params);
            container.addView(chip);
        }
    }

    private void showReminderEditorDialog(@Nullable ReminderScheduler.ReminderItem editingItem,
                                          @NonNull LinearLayout listContainer) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder_slot, null, false);
        android.widget.TimePicker timePicker = dialogView.findViewById(R.id.tp_reminder_time);
        Spinner typeSpinner = dialogView.findViewById(R.id.spinner_reminder_type);
        Spinner alarmSoundSpinner = dialogView.findViewById(R.id.spinner_alarm_sound_dialog);
        View alarmSoundSection = dialogView.findViewById(R.id.layout_alarm_sound_dialog);
        View previewAlarmButton = dialogView.findViewById(R.id.btn_preview_alarm_sound_dialog);
        View stopPreviewAlarmButton = dialogView.findViewById(R.id.btn_stop_preview_alarm_sound_dialog);
        SwitchCompat enabledSwitch = dialogView.findViewById(R.id.switch_reminder_enabled);

        timePicker.setIs24HourView(true);

        int hour = editingItem != null ? editingItem.hour : 8;
        int minute = editingItem != null ? editingItem.minute : 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.reminder_type_labels,
                android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        int currentType = editingItem != null ? editingItem.type : ReminderScheduler.TYPE_NOTIFICATION;
        typeSpinner.setSelection(currentType == ReminderScheduler.TYPE_ALARM ? 1 : 0, false);

        ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.alarm_sound_labels,
            android.R.layout.simple_spinner_item
        );
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alarmSoundSpinner.setAdapter(soundAdapter);

        String selectedSound = editingItem != null ? editingItem.alarmSound
            : getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(ReminderScheduler.PREF_ALARM_SOUND, ReminderScheduler.DEFAULT_ALARM_SOUND);
        alarmSoundSpinner.setSelection(findSoundIndex(selectedSound), false);
        previewAlarmButton.setOnClickListener(v ->
            playPreviewSound(ALARM_SOUND_VALUES[Math.max(0, Math.min(ALARM_SOUND_VALUES.length - 1,
                alarmSoundSpinner.getSelectedItemPosition()))])
        );
        stopPreviewAlarmButton.setOnClickListener(v -> stopPreviewSound());

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            alarmSoundSection.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        alarmSoundSection.setVisibility(currentType == ReminderScheduler.TYPE_ALARM ? View.VISIBLE : View.GONE);

        int repeatMask = editingItem != null
            ? editingItem.repeatMask
            : ReminderScheduler.getRepeatMask(this);
        applyRepeatMaskToCheckboxes(dialogView, DIALOG_REPEAT_DAY_CHECKBOX_IDS, repeatMask);

        enabledSwitch.setChecked(editingItem == null || editingItem.enabled);

        int titleRes = editingItem == null ? R.string.add : R.string.edit;
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    int pickedHour;
                    int pickedMinute;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        pickedHour = timePicker.getHour();
                        pickedMinute = timePicker.getMinute();
                    } else {
                        pickedHour = timePicker.getCurrentHour();
                        pickedMinute = timePicker.getCurrentMinute();
                    }

                    ReminderScheduler.ReminderItem item = editingItem == null
                            ? new ReminderScheduler.ReminderItem()
                            : cloneReminderItem(editingItem);

                    if (item.id <= 0) {
                        item.id = ReminderScheduler.allocateReminderId(this);
                    }
                    item.hour = pickedHour;
                    item.minute = pickedMinute;
                    item.type = typeSpinner.getSelectedItemPosition() == 1
                            ? ReminderScheduler.TYPE_ALARM
                            : ReminderScheduler.TYPE_NOTIFICATION;
                    item.enabled = enabledSwitch.isChecked();
                        item.repeatMask = collectRepeatMask(dialogView, DIALOG_REPEAT_DAY_CHECKBOX_IDS);
                        if (item.repeatMask == 0) {
                        item.repeatMask = ReminderScheduler.REPEAT_MASK_ALL_DAYS;
                        }
                        item.alarmSound = ALARM_SOUND_VALUES[Math.max(0, Math.min(ALARM_SOUND_VALUES.length - 1,
                            alarmSoundSpinner.getSelectedItemPosition()))];

                    ReminderScheduler.upsertReminder(this, item);
                    syncRemindersToCloudIfLoggedIn();
                    renderReminderList(listContainer);
                    Toast.makeText(this, R.string.reminder_saved, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private ReminderScheduler.ReminderItem cloneReminderItem(ReminderScheduler.ReminderItem source) {
        ReminderScheduler.ReminderItem item = new ReminderScheduler.ReminderItem();
        item.id = source.id;
        item.hour = source.hour;
        item.minute = source.minute;
        item.type = source.type;
        item.enabled = source.enabled;
        item.repeatMask = source.repeatMask;
        item.alarmSound = source.alarmSound;
        return item;
    }

    private String formatReminderType(int type) {
        return type == ReminderScheduler.TYPE_ALARM
                ? getString(R.string.reminder_type_alarm)
                : getString(R.string.reminder_type_notification);
    }

    private String formatReminderDetails(ReminderScheduler.ReminderItem item) {
        if (item.type == ReminderScheduler.TYPE_ALARM) {
            String soundLabel = getSoundLabel(item.alarmSound);
            return formatReminderType(item.type) + " | " + soundLabel;
        }
        return formatReminderType(item.type);
    }

    private String buildRepeatSummary(int repeatMask) {
        int normalizedMask = ReminderScheduler.normalizeRepeatMask(repeatMask);
        if (normalizedMask == ReminderScheduler.REPEAT_MASK_ALL_DAYS) {
            return getString(R.string.reminder_repeat_everyday);
        }

        List<String> dayLabels = new ArrayList<>();
        for (int i = 0; i < WEEKDAY_LABEL_IDS.length; i++) {
            if ((normalizedMask & (1 << i)) != 0) {
                dayLabels.add(getString(WEEKDAY_LABEL_IDS[i]));
            }
        }
        return TextUtils.join(", ", dayLabels);
    }

    private String getSoundLabel(String soundValue) {
        int index = findSoundIndex(soundValue);
        String[] labels = getResources().getStringArray(R.array.alarm_sound_labels);
        if (index >= 0 && index < labels.length) {
            return labels[index];
        }
        return labels[0];
    }

    private String formatReminderTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private int collectRepeatMask(View rootView, int[] checkboxIds) {
        int mask = 0;
        for (int i = 0; i < checkboxIds.length; i++) {
            CompoundButton checkBox = rootView.findViewById(checkboxIds[i]);
            if (checkBox.isChecked()) {
                mask |= (1 << i);
            }
        }
        return mask;
    }

    private void applyRepeatMaskToCheckboxes(View rootView, int[] checkboxIds, int repeatMask) {
        int normalizedMask = ReminderScheduler.normalizeRepeatMask(repeatMask);
        for (int i = 0; i < checkboxIds.length; i++) {
            CompoundButton checkBox = rootView.findViewById(checkboxIds[i]);
            checkBox.setChecked((normalizedMask & (1 << i)) != 0);
        }
    }

    private void refreshActiveScreen() {
        if (bottomNavigationView == null) {
            return;
        }
        int currentItem = bottomNavigationView.getSelectedItemId();
        showScreen(currentItem == 0 ? R.id.nav_today : currentItem);
    }

    private int findSoundIndex(String value) {
        for (int i = 0; i < ALARM_SOUND_VALUES.length; i++) {
            if (ALARM_SOUND_VALUES[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void applySavedThemeAndLanguage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean darkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        String languageTag = prefs.getString(PREF_LANGUAGE, SUPPORTED_LANGUAGES[0]);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
    }

    private int getLanguageIndex(String languageTag) {
        for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
            if (SUPPORTED_LANGUAGES[i].equals(languageTag)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        stopPreviewSound();
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    private interface AmountConsumer {
        void onAmount(int amount);
    }

    private static class HydrationLogEntry {
        final long timestamp;
        final int amountMl;

        HydrationLogEntry(long timestamp, int amountMl) {
            this.timestamp = timestamp;
            this.amountMl = amountMl;
        }
    }

    private static class WaterRecommendation {
        final int minMl;
        final int maxMl;
        final int targetMl;

        WaterRecommendation(int minMl, int maxMl, int targetMl) {
            this.minMl = minMl;
            this.maxMl = maxMl;
            this.targetMl = targetMl;
        }
    }

    private static class ExploreArticle {
        final String title;
        final String summary;
        final String url;
        final String readTime;

        ExploreArticle(String title, String summary, String url, String readTime) {
            this.title = title;
            this.summary = summary;
            this.url = url;
            this.readTime = readTime;
        }
    }

    private static class ExploreTool {
        final int id;
        final String title;
        final String description;

        ExploreTool(int id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }
    }
}