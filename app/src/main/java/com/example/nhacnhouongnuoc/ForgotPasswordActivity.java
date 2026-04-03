package com.example.nhacnhouongnuoc;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String EMAILJS_ENDPOINT = "https://api.emailjs.com/api/v1.0/email/send";
    private static final String EMAILJS_SERVICE_ID = "service_obzv6qf";
    private static final String EMAILJS_TEMPLATE_ID = "template_q7pgamh";
    private static final String EMAILJS_PUBLIC_KEY = "KzlLQMo1tqptozf4_";
    private static final String EMAILJS_PRIVATE_KEY = "h8O8b8DXnmyKEN4mnHy8n";
    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SecureRandom random = new SecureRandom();

    private FirebaseAuth firebaseAuth;

    private EditText etEmail;
    private EditText etOtp;
    private EditText etNewPassword;
    private EditText etConfirmNewPassword;

    private String generatedOtp;
    private long otpExpireAt;
    private String otpEmail;
    private boolean otpVerified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_forgot_email);
        etOtp = findViewById(R.id.et_forgot_otp);
        etNewPassword = findViewById(R.id.et_forgot_new_password);
        etConfirmNewPassword = findViewById(R.id.et_forgot_confirm_new_password);

        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirmNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (prefillEmail != null && !prefillEmail.trim().isEmpty()) {
            etEmail.setText(prefillEmail.trim());
        }

        Button btnSendOtp = findViewById(R.id.btn_send_otp);
        Button btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        Button btnSendReset = findViewById(R.id.btn_send_reset_link);

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnSendReset.setOnClickListener(v -> resetPasswordAfterOtp());
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void sendOtp() {
        String email = etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));

        executor.execute(() -> {
            try {
                JSONObject params = new JSONObject();
                params.put("email", email);
                params.put("OTP", otp);

                JSONObject payload = new JSONObject();
                payload.put("service_id", EMAILJS_SERVICE_ID);
                payload.put("template_id", EMAILJS_TEMPLATE_ID);
                payload.put("user_id", EMAILJS_PUBLIC_KEY);
                payload.put("accessToken", EMAILJS_PRIVATE_KEY);
                payload.put("template_params", params);

                URL url = new URL(EMAILJS_ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                byte[] out = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(out);
                }

                int code = connection.getResponseCode();
                connection.disconnect();

                if (code >= 200 && code < 300) {
                    generatedOtp = otp;
                    otpExpireAt = System.currentTimeMillis() + OTP_VALIDITY_MS;
                    otpEmail = email;
                    otpVerified = false;
                    runOnUiThread(() -> Toast.makeText(this, R.string.otp_sent, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, R.string.otp_send_failed, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.auth_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void verifyOtp() {
        String email = etEmail.getText().toString().trim();
        String inputOtp = etOtp.getText().toString().trim();

        if (generatedOtp == null || otpEmail == null) {
            Toast.makeText(this, R.string.request_otp_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equalsIgnoreCase(otpEmail)) {
            Toast.makeText(this, R.string.otp_email_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        if (System.currentTimeMillis() > otpExpireAt) {
            otpVerified = false;
            Toast.makeText(this, R.string.otp_expired, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!generatedOtp.equals(inputOtp)) {
            otpVerified = false;
            Toast.makeText(this, R.string.otp_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        otpVerified = true;
        Toast.makeText(this, R.string.otp_verified, Toast.LENGTH_SHORT).show();
    }

    private void resetPasswordAfterOtp() {
        String email = etEmail.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmNewPassword.getText().toString().trim();

        if (!otpVerified) {
            Toast.makeText(this, R.string.verify_otp_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equalsIgnoreCase(otpEmail)) {
            Toast.makeText(this, R.string.otp_email_mismatch, Toast.LENGTH_SHORT).show();
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

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(unused -> {
                        FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .child(user.getUid())
                                .child("security")
                                .child("passwordUpdatedAt")
                                .setValue(System.currentTimeMillis());
                        Toast.makeText(this, R.string.password_updated_success, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.auth_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            return;
        }

        // Firebase Auth requires an authenticated context (or reset-link flow) to change another account password.
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(this, R.string.reset_requires_email_link, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.auth_failed, e.getMessage()), Toast.LENGTH_LONG).show());
    }
}
