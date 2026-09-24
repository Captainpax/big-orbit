package com.littleorbit.bigorbit;

import android.content.Context;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** Device enrollment, proof-of-possession, and short-session rotation workflow. */
public final class AdminSessionCoordinator {
    public enum LoginResult { SIGNED_IN, MFA_REQUIRED }
    private final ApiClient api = new ApiClient();
    private final DeviceKeyStore keys = new DeviceKeyStore();
    private final Context context;
    private final SecureSessionStore sessions;

    public AdminSessionCoordinator(Context context) {
        this.context = context.getApplicationContext();
        sessions = new SecureSessionStore(this.context);
    }

    public boolean isEnrolled() {
        return sessions.read() != null;
    }

    public boolean isSignedIn() {
        SecureSessionStore.State state = sessions.read();
        return state != null && !state.accessToken().isBlank();
    }

    public boolean hasBootstrap() {
        return sessions.readBootstrap() != null;
    }

    public String currentDeviceId() {
        SecureSessionStore.State state = sessions.read();
        return state == null ? null : state.deviceId();
    }

    public LoginResult login(
            String email, String password, String mfaProof, String deviceLabel) throws Exception {
        SecureSessionStore.State state = sessions.read();
        if (state == null) {
            return bootstrap(email, password, mfaProof, deviceLabel);
        } else {
            passwordSession(email, password, mfaProof, state);
            return LoginResult.SIGNED_IN;
        }
    }

    public String validToken() throws Exception {
        SecureSessionStore.State state = sessions.read();
        if (state == null) throw new ApiException(403);
        if (state.accessToken().isBlank()) {
            throw new IllegalStateException("Big Orbit is locally signed out");
        }
        return state.accessToken();
    }

    public JSONObject authorizedObject(String path) throws Exception {
        try {
            return api.getObject(path, validToken());
        } catch (ApiException failure) {
            if (!reauthenticate(failure)) throw failure;
            return api.getObject(path, refresh().accessToken());
        }
    }

    public JSONArray authorizedArray(String path) throws Exception {
        try {
            return api.getArray(path, validToken());
        } catch (ApiException failure) {
            if (!reauthenticate(failure)) throw failure;
            return api.getArray(path, refresh().accessToken());
        }
    }

    public JSONObject authorizedPost(String path, JSONObject body) throws Exception {
        try {
            return api.post(path, body, validToken());
        } catch (ApiException failure) {
            if (!reauthenticate(failure)) throw failure;
            return api.post(path, body, refresh().accessToken());
        }
    }

    public JSONObject authorizedPut(String path, JSONObject body) throws Exception {
        try {
            return api.put(path, body, validToken());
        } catch (ApiException failure) {
            if (!reauthenticate(failure)) throw failure;
            return api.put(path, body, refresh().accessToken());
        }
    }

    public JSONObject authorizedPatch(String path, JSONObject body) throws Exception {
        try {
            return api.patch(path, body, validToken());
        } catch (ApiException failure) {
            if (!reauthenticate(failure)) throw failure;
            return api.patch(path, body, refresh().accessToken());
        }
    }

    public JSONObject authorizedDelete(String path) throws Exception {
        try {
            return api.delete(path, null, validToken());
        } catch (ApiException failure) {
            if (!reauthenticate(failure)) throw failure;
            return api.delete(path, null, refresh().accessToken());
        }
    }

    public void signOut() {
        sessions.clearAccessToken();
        NotificationManagerCompat.from(context).cancelAll();
    }

    public void forgetRevokedDevice() {
        sessions.clear();
        try {
            keys.delete();
        } catch (Exception ignored) {
            // The server-side revocation is authoritative even if local key deletion fails.
        }
    }

    private LoginResult bootstrap(
            String email, String password, String pin, String deviceLabel) throws Exception {
        JSONObject request = new JSONObject()
                .put("email", email)
                .put("password", password)
                .put("pin", pin)
                .put("enrollment_public_key", keys.publicKeySpki())
                .put("device_label", deviceLabel);
        JSONObject response = api.post("/v2/admin/bootstrap/session", request, null);
        String token = response.getString("setup_token");
        String deviceId = response.getString("device_id");
        sessions.saveBootstrap(new SecureSessionStore.BootstrapState(token, deviceId));
        JSONObject challenge = response.getJSONObject("challenge");
        String challengeId = challenge.getString("challenge_id");
        String raw = challenge.getString("challenge");
        JSONObject confirm = new JSONObject()
                .put("challenge_id", challengeId)
                .put("challenge", raw)
                .put("signature", keys.sign("bootstrap", challengeId, raw));
        JSONObject result = api.post("/v2/admin/bootstrap/device-confirm", confirm, token);
        if (!result.optBoolean("completed")) return LoginResult.MFA_REQUIRED;
        saveCompletion(result.getJSONObject("completion"));
        return LoginResult.SIGNED_IN;
    }

    void saveCompletion(JSONObject completion) throws Exception {
        sessions.save(new SecureSessionStore.State(
                completion.getString("device_id"),
                completion.getString("device_credential"),
                completion.getString("access_token")));
        sessions.clearBootstrap();
    }

    private void passwordSession(
            String email, String password, String proof, SecureSessionStore.State state)
            throws Exception {
        JSONObject challenge = challenge(state.deviceId());
        String challengeId = challenge.getString("challenge_id");
        String raw = challenge.getString("challenge");
        JSONObject request = credentials(email, password, proof)
                .put("device_id", state.deviceId())
                .put("challenge_id", challengeId)
                .put("challenge", raw)
                .put("signature", keys.sign("session", challengeId, raw));
        JSONObject response = api.post("/v2/admin/session", request, null);
        sessions.save(new SecureSessionStore.State(
                state.deviceId(), state.deviceCredential(), response.getString("access_token")));
    }

    private SecureSessionStore.State refresh() throws Exception {
        SecureSessionStore.State state = sessions.read();
        if (state == null) throw new ApiException(403);
        JSONObject challenge = challenge(state.deviceId());
        String challengeId = challenge.getString("challenge_id");
        String raw = challenge.getString("challenge");
        JSONObject request = new JSONObject()
                .put("device_id", state.deviceId())
                .put("device_credential", state.deviceCredential())
                .put("challenge_id", challengeId)
                .put("challenge", raw)
                .put("signature", keys.sign("session", challengeId, raw));
        JSONObject response = api.post("/v2/admin/device-session", request, null);
        SecureSessionStore.State updated = new SecureSessionStore.State(
                state.deviceId(), state.deviceCredential(), response.getString("access_token"));
        if (!sessions.replaceIfCurrent(state, updated)) {
            throw new IllegalStateException("Big Orbit session changed during refresh");
        }
        return updated;
    }

    private JSONObject challenge(String deviceId) throws Exception {
        return api.post(
                "/v2/admin/device-challenges", new JSONObject().put("device_id", deviceId), null);
    }

    private static JSONObject credentials(String email, String password, String proof)
            throws Exception {
        JSONObject value = new JSONObject().put("email", email).put("password", password);
        if (proof.matches("^[0-9]{6}$")) return value.put("totp_code", proof);
        return value.put("recovery_code", proof);
    }

    private static boolean reauthenticate(ApiException failure) {
        return failure.statusCode() == 401 || failure.statusCode() == 403;
    }
}
