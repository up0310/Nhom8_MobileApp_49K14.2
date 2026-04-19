package com.example.nhacnhouongnuoc;

import android.os.Bundle;
import android.text.InputType;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;

    private TextView tvStatus;
    private EditText etEmail;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        firebaseAuth = FirebaseAuth.getInstance();

        tvStatus = findViewById(R.id.tv_auth_status);
        etEmail = findViewById(R.id.et_auth_email);
        etPassword = findViewById(R.id.et_auth_password);

        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Button btnLogin = findViewById(R.id.btn_auth_login);
        Button btnRegister = findViewById(R.id.btn_auth_register);
        Button btnForgot = findViewById(R.id.btn_auth_forgot);

        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnForgot.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            intent.putExtra("prefill_email", etEmail.getText().toString().trim());
            startActivity(intent);
        });

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, R.string.input_required, Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    updateStatus();
                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, resolveAuthError(e), Toast.LENGTH_LONG).show());
    }

    private String resolveAuthError(Exception e) {
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
        }

        return getString(R.string.auth_failed, e.getMessage());
    }

    private void updateStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            tvStatus.setText(R.string.account_not_logged_in);
            return;
        }

        tvStatus.setText(getString(R.string.account_logged_in_as, user.getEmail()));
        etEmail.setText(user.getEmail());
    }
}
