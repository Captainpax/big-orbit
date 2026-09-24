package com.littleorbit.bigorbit;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Resumes the restricted PIN bootstrap without ever holding an administrator session. */
public final class MfaEnrollmentCoordinator {
    private final ApiClient api = new ApiClient();
    private final DeviceKeyStore keys = new DeviceKeyStore();
    private final SecureSessionStore store;
    private final AdminSessionCoordinator sessions;

    public MfaEnrollmentCoordinator(Context context) {
        store = new SecureSessionStore(context.getApplicationContext());
        sessions = new AdminSessionCoordinator(context);
    }

    public ResumeResult resume() throws Exception {
        SecureSessionStore.BootstrapState bootstrap = requireBootstrap();
        try {
            JSONObject status = api.getObject("/v2/admin/bootstrap/status", bootstrap.setupToken());
            if (!status.getBoolean("device_confirmed")) {
                status = confirmDevice(bootstrap);
            }
            if (!status.getBoolean("mfa_required")) {
                if (!sessions.isEnrolled()) confirmDevice(bootstrap);
                return new ResumeResult("", "", true);
            }
            JSONObject enrollment = status.getJSONObject("mfa_enrollment");
            return new ResumeResult(
                    enrollment.getString("otpauth_uri"),
                    enrollment.getString("qr_png_data_url"),
                    false);
        } catch (ApiException failure) {
            clearExpired(failure);
            throw failure;
        }
    }

    public JSONArray confirm(String code) throws Exception {
        SecureSessionStore.BootstrapState bootstrap = requireBootstrap();
        try {
            JSONObject completion = api.post(
                    "/v2/admin/bootstrap/mfa/confirm",
                    new JSONObject().put("code", code),
                    bootstrap.setupToken());
            sessions.saveCompletion(completion);
            return completion.getJSONArray("recovery_codes");
        } catch (ApiException failure) {
            clearExpired(failure);
            throw failure;
        }
    }

    private JSONObject confirmDevice(SecureSessionStore.BootstrapState bootstrap)
            throws Exception {
        JSONObject challenge = api.post(
                "/v2/admin/bootstrap/challenge", new JSONObject(), bootstrap.setupToken());
        String challengeId = challenge.getString("challenge_id");
        String raw = challenge.getString("challenge");
        JSONObject proof = new JSONObject()
                .put("challenge_id", challengeId)
                .put("challenge", raw)
                .put("signature", keys.sign("bootstrap", challengeId, raw));
        JSONObject result = api.post(
                "/v2/admin/bootstrap/device-confirm", proof, bootstrap.setupToken());
        if (result.optBoolean("completed")) {
            sessions.saveCompletion(result.getJSONObject("completion"));
            return new JSONObject().put("mfa_required", false);
        }
        return result.getJSONObject("status");
    }

    private SecureSessionStore.BootstrapState requireBootstrap() {
        SecureSessionStore.BootstrapState bootstrap = store.readBootstrap();
        if (bootstrap == null) {
            throw new IllegalStateException("Bootstrap session is unavailable");
        }
        return bootstrap;
    }

    private void clearExpired(ApiException failure) {
        if (failure.statusCode() == 401 || failure.statusCode() == 403) {
            store.clearBootstrap();
        }
    }

    /** A resumed setup either needs its QR screen or already recovered ordinary credentials. */
    public record ResumeResult(
            String authenticatorUri, String qrPngDataUrl, boolean signedIn) {}
}
