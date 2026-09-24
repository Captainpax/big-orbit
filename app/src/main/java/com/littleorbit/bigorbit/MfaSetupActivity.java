package com.littleorbit.bigorbit;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/** QR-based first-owner MFA setup within the restricted PIN bootstrap capability. */
public final class MfaSetupActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private MfaEnrollmentCoordinator coordinator;
    private String authenticatorUri;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_mfa_setup);
        coordinator = new MfaEnrollmentCoordinator(this);
        findViewById(R.id.openAuthenticatorButton).setOnClickListener(
                ignored -> openAuthenticator());
        findViewById(R.id.confirmMfaButton).setOnClickListener(ignored -> confirm());
        findViewById(R.id.finishMfaButton).setOnClickListener(ignored -> openDashboard());
        resumeSetup();
    }

    private void resumeSetup() {
        findViewById(R.id.returnToLoginButton).setVisibility(View.GONE);
        busy(true, getString(R.string.mfa_loading));
        executor.execute(() -> {
            try {
                MfaEnrollmentCoordinator.ResumeResult result = coordinator.resume();
                main.post(() -> {
                    if (result.signedIn()) openDashboard();
                    else showEnrollment(result);
                });
            } catch (ApiException failure) {
                int message = failure.statusCode() == 401 || failure.statusCode() == 403
                        ? R.string.bootstrap_expired : R.string.mfa_enrollment_unavailable;
                main.post(() -> unavailable(message));
            } catch (Exception failure) {
                main.post(() -> unavailable(R.string.mfa_enrollment_unavailable));
            }
        });
    }

    private void showEnrollment(MfaEnrollmentCoordinator.ResumeResult result) {
        authenticatorUri = result.authenticatorUri();
        TextView uri = findViewById(R.id.authenticatorUri);
        uri.setText(authenticatorUri);
        uri.setVisibility(View.VISIBLE);
        Bitmap bitmap;
        try {
            byte[] png = decodePng(result.qrPngDataUrl());
            bitmap = BitmapFactory.decodeByteArray(png, 0, png.length);
            if (bitmap == null) throw new IllegalArgumentException("Invalid QR image bytes");
        } catch (RuntimeException invalidImage) {
            unavailable(R.string.mfa_enrollment_unavailable);
            return;
        }
        ((ImageView) findViewById(R.id.authenticatorQr)).setImageBitmap(bitmap);
        findViewById(R.id.enrollmentControls).setVisibility(View.VISIBLE);
        busy(false, getString(R.string.mfa_ready));
    }

    private void confirm() {
        String code = input(R.id.confirmCodeInput);
        if (!code.matches("^[0-9]{6}$")) {
            status(getString(R.string.mfa_code_required));
            return;
        }
        busy(true, getString(R.string.mfa_confirming));
        executor.execute(() -> {
            try {
                JSONArray codes = coordinator.confirm(code);
                main.post(() -> showRecoveryCodes(codes));
            } catch (Exception failure) {
                main.post(() -> busy(false, getString(R.string.mfa_code_rejected)));
            }
        });
    }

    private void showRecoveryCodes(JSONArray codes) {
        StringBuilder message = new StringBuilder(getString(R.string.recovery_codes));
        for (int index = 0; index < codes.length(); index++) {
            message.append('\n').append(codes.optString(index));
        }
        findViewById(R.id.enrollmentControls).setVisibility(View.GONE);
        findViewById(R.id.finishMfaButton).setVisibility(View.VISIBLE);
        busy(false, message.toString());
    }

    private void openAuthenticator() {
        if (authenticatorUri == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authenticatorUri)));
        } catch (ActivityNotFoundException missing) {
            status(getString(R.string.no_authenticator));
        }
    }

    private void unavailable(int message) {
        busy(false, getString(message));
        android.widget.Button action = findViewById(R.id.returnToLoginButton);
        boolean expired = message == R.string.bootstrap_expired;
        action.setText(expired ? R.string.return_to_login : R.string.try_again);
        action.setVisibility(View.VISIBLE);
        action.setOnClickListener(ignored -> {
            if (expired) finish();
            else resumeSetup();
        });
    }

    private void openDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void busy(boolean value, String message) {
        findViewById(R.id.mfaProgress).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.confirmMfaButton).setEnabled(!value);
        status(message);
    }

    private String input(int id) {
        TextInputEditText value = findViewById(id);
        return value.getText() == null ? "" : value.getText().toString().trim();
    }

    private void status(String value) {
        ((TextView) findViewById(R.id.mfaStatus)).setText(value);
    }

    private static byte[] decodePng(String dataUrl) {
        int separator = dataUrl.indexOf(',');
        if (separator < 0 || !dataUrl.startsWith("data:image/png;base64,")) {
            throw new IllegalArgumentException("Invalid QR image");
        }
        return Base64.decode(dataUrl.substring(separator + 1), Base64.DEFAULT);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
