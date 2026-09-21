package com.littleorbit.bigorbit;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/** Authenticator enrollment without putting an owner panel in the public website. */
public final class MfaSetupActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MfaEnrollmentCoordinator coordinator = new MfaEnrollmentCoordinator();
    private MfaEnrollmentCoordinator.Challenge challenge;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_mfa_setup);
        findViewById(R.id.startMfaButton).setOnClickListener(ignored -> begin());
        findViewById(R.id.confirmMfaButton).setOnClickListener(ignored -> confirm());
        findViewById(R.id.openAuthenticatorButton).setOnClickListener(ignored -> {
            if (challenge != null) {
                try {
                    startActivity(new Intent(
                            Intent.ACTION_VIEW, Uri.parse(challenge.authenticatorUri())));
                } catch (ActivityNotFoundException missingAuthenticator) {
                    status("No authenticator app could open this link. Copy it manually.");
                }
            }
        });
    }

    private void begin() {
        String email = input(R.id.mfaEmailInput, true);
        String password = input(R.id.mfaPasswordInput, false);
        if (email.isBlank() || password.isBlank()) {
            status("Enter your owner email and password.");
            return;
        }
        status("Creating a private authenticator enrollment…");
        executor.execute(() -> {
            try {
                MfaEnrollmentCoordinator.Challenge result = coordinator.begin(email, password);
                main.post(() -> showChallenge(result));
            } catch (Exception failure) {
                main.post(() -> status("MFA enrollment could not be started."));
            }
        });
    }

    private void showChallenge(MfaEnrollmentCoordinator.Challenge result) {
        challenge = result;
        TextView uri = findViewById(R.id.authenticatorUri);
        uri.setText(result.authenticatorUri());
        uri.setVisibility(View.VISIBLE);
        findViewById(R.id.openAuthenticatorButton).setVisibility(View.VISIBLE);
        findViewById(R.id.confirmCodeContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.confirmMfaButton).setVisibility(View.VISIBLE);
        status("Authenticator link ready. Confirm one current code.");
    }

    private void confirm() {
        String code = input(R.id.confirmCodeInput, true);
        if (challenge == null || !code.matches("^[0-9]{6}$")) {
            status("Enter the current six-digit authenticator code.");
            return;
        }
        status("Confirming MFA…");
        executor.execute(() -> {
            try {
                JSONArray codes = coordinator.confirm(challenge, code);
                main.post(() -> showRecoveryCodes(codes));
            } catch (Exception failure) {
                main.post(() -> status("That code was not accepted."));
            }
        });
    }

    private void showRecoveryCodes(JSONArray codes) {
        StringBuilder message = new StringBuilder(getString(R.string.recovery_codes));
        for (int index = 0; index < codes.length(); index++) {
            message.append("\n").append(codes.optString(index));
        }
        status(message.toString());
        findViewById(R.id.confirmMfaButton).setEnabled(false);
    }

    private String input(int id, boolean trim) {
        TextInputEditText value = findViewById(id);
        String text = value.getText() == null ? "" : value.getText().toString();
        return trim ? text.trim() : text;
    }

    private void status(String value) {
        ((TextView) findViewById(R.id.mfaStatus)).setText(value);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
