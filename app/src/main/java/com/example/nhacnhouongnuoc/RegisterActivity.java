package com.example.nhacnhouongnuoc;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;

    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);

        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        btnCreate = findViewById(R.id.btn_register_create_account);
        Button btnBackToLogin = findViewById(R.id.btn_register_back_login);
        android.view.View btnBackTop = findViewById(R.id.btn_register_back);

        btnCreate.setOnClickListener(v -> register());
        btnBackToLogin.setOnClickListener(v -> finish());
        btnBackTop.setOnClickListener(v -> finish());
    }

    private void register() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty() || password.length() < 6 || confirmPassword.length() < 6) {
            Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.password_not_match, Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setAlpha(0.6f);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    btnCreate.setEnabled(true);
                    btnCreate.setAlpha(1f);
                    showRegisterNotice(true, getString(R.string.register_success_popup));
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    btnCreate.setAlpha(1f);
                    showRegisterNotice(false, resolveRegisterError(e));
                });
    }

    private void showRegisterNotice(boolean success, String message) {
        final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
        dialog.setContentView(R.layout.dialog_status_notice);

        TextView iconView = dialog.findViewById(R.id.tv_notice_icon);
        TextView titleView = dialog.findViewById(R.id.tv_notice_title);
        TextView messageView = dialog.findViewById(R.id.tv_notice_message);
        MaterialButton closeButton = dialog.findViewById(R.id.btn_notice_close);

        if (success) {
            iconView.setText("\u2713");
            iconView.setTextColor(0xFF2EB82E);
            titleView.setText(R.string.notice_success_title);
            closeButton.setText(R.string.close);
            closeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
            closeButton.setTextColor(0xFF3A3A3A);
        } else {
            iconView.setText("!");
            iconView.setTextColor(0xFFE74C3C);
            titleView.setText(R.string.notice_error_title);
            closeButton.setText(R.string.close);
            closeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
            closeButton.setTextColor(0xFF3A3A3A);
        }

        messageView.setText(message);
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (success) {
                finish();
            }
        });

        dialog.show();
    }

    private String resolveRegisterError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return getString(R.string.network_error_try_again);
        }

        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(errorCode)
                    || "ERROR_INTERNAL_ERROR".equals(errorCode)) {
                String message = e.getMessage() == null ? "" : e.getMessage();
                if (message.contains("CONFIGURATION_NOT_FOUND")
                        || message.contains("operation is not allowed")) {
                    return getString(R.string.firebase_auth_configuration_missing);
                }
            }
            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(errorCode)) {
                return getString(R.string.email_already_in_use);
            }
            if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
                return getString(R.string.invalid_email);
            }
            if ("ERROR_WEAK_PASSWORD".equals(errorCode)) {
                return getString(R.string.password_too_short);
            }
        }

        return getString(R.string.auth_failed, e.getMessage());
    }
}
