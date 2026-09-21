package com.littleorbit.bigorbit;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Device enrollment, proof-of-possession, and short-session rotation workflow. */
public final class AdminSessionCoordinator {
    private final ApiClient api = new ApiClient();
    private final DeviceKeyStore keys = new DeviceKeyStore();
    private final SecureSessionStore sessions;

    public AdminSessionCoordinator(Context context) {
        sessions = new SecureSessionStore(context.getApplicationContext());
    }

    public boolean isEnrolled() {
        return sessions.read() != null;
    }

    public String currentDeviceId() {
        SecureSessionStore.State state = sessions.read();
        return state == null ? null : state.deviceId();
    }

    public void login(
            String email, String password, String mfaProof, String deviceLabel) throws Exception {
        SecureSessionStore.State state = sessions.read();
        if (state == null) {
            enroll(email, password, mfaProof, deviceLabel);
        } else {
            passwordSession(email, password, mfaProof, state);
        }
    }

    public String validToken() throws Exception {
        SecureSessionStore.State state = sessions.read();
        if (state == null) throw new ApiException(403);
        if (!state.accessToken().isBlank()) return state.accessToken();
        return refresh().accessToken();
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
    }

    public void forgetRevokedDevice() {
        sessions.clear();
        try {
            keys.delete();
        } catch (Exception ignored) {
            // The server-side revocation is authoritative even if local key deletion fails.
        }
    }

    private void enroll(
            String email, String password, String proof, String deviceLabel) throws Exception {
        JSONObject request = credentials(email, password, proof)
                .put("enrollment_public_key", keys.publicKeySpki())
                .put("device_label", deviceLabel);
        JSONObject response = api.post("/v2/admin/session", request, null);
        String token = response.getString("access_token");
        String deviceId = response.getString("device_id");
        JSONObject challenge = response.getJSONObject("enrollment_challenge");
        String challengeId = challenge.getString("challenge_id");
        String raw = challenge.getString("challenge");
        JSONObject confirm = new JSONObject()
                .put("challenge_id", challengeId)
                .put("challenge", raw)
                .put("signature", keys.sign("enrollment", challengeId, raw));
        JSONObject enrolled = api.post(
                "/v2/admin/devices/" + deviceId + "/confirm", confirm, token);
        sessions.save(new SecureSessionStore.State(
                deviceId, enrolled.getString("device_credential"), token));
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
        sessions.save(updated);
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
