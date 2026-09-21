package com.littleorbit.bigorbit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owner sign-in and first-device enrollment. */
public final class LoginActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText proof;
    private TextInputEditText deviceLabel;
    private View progress;
    private android.widget.TextView status;
    private AdminSessionCoordinator sessions;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_login);
        sessions = new AdminSessionCoordinator(this);
        email = findViewById(R.id.emailInput);
        password = findViewById(R.id.passwordInput);
        proof = findViewById(R.id.mfaInput);
        deviceLabel = findViewById(R.id.deviceLabelInput);
        progress = findViewById(R.id.loginProgress);
        status = findViewById(R.id.loginStatus);
        findViewById(R.id.deviceLabelContainer).setVisibility(
                sessions.isEnrolled() ? View.GONE : View.VISIBLE);
        if (!sessions.isEnrolled()) deviceLabel.setText(android.os.Build.MODEL);
        findViewById(R.id.loginButton).setOnClickListener(ignored -> submit());
        findViewById(R.id.mfaSetupButton).setOnClickListener(ignored ->
                startActivity(new Intent(this, MfaSetupActivity.class)));
    }

    private void submit() {
        String emailValue = text(email, true);
        String passwordValue = text(password, false);
        String proofValue = text(proof, true);
        String labelValue = text(deviceLabel, true);
        if (emailValue.isBlank() || passwordValue.isBlank() || proofValue.isBlank()
                || (!sessions.isEnrolled() && labelValue.isBlank())) {
            status.setText(R.string.complete_fields);
            return;
        }
        busy(true);
        executor.execute(() -> {
            try {
                sessions.login(emailValue, passwordValue, proofValue, labelValue);
                main.post(this::openDashboard);
            } catch (Exception failure) {
                main.post(() -> {
                    busy(false);
                    status.setText(R.string.sign_in_rejected);
                });
            }
        });
    }

    private void openDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void busy(boolean value) {
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.loginButton).setEnabled(!value);
        status.setText(value ? getString(R.string.verifying_device) : "");
    }

    private static String text(TextInputEditText input, boolean trim) {
        String value = input.getText() == null ? "" : input.getText().toString();
        return trim ? value.trim() : value;
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
