package com.littleorbit.bigorbit;

import org.json.JSONArray;
import org.json.JSONObject;

/** First-owner MFA bootstrap performed inside Big Orbit, not a browser. */
public final class MfaEnrollmentCoordinator {
    private final ApiClient api = new ApiClient();

    public Challenge begin(String email, String password) throws Exception {
        JSONObject login = api.post(
                "/v1/auth/login",
                new JSONObject().put("email", email).put("password", password),
                null);
        String token = login.getString("access_token");
        JSONObject challenge = api.post(
                "/v2/admin/mfa/start",
                new JSONObject().put("password", password),
                token);
        return new Challenge(token, challenge.getString("otpauth_uri"));
    }

    public JSONArray confirm(Challenge challenge, String code) throws Exception {
        JSONObject result = api.post(
                "/v2/admin/mfa/confirm",
                new JSONObject().put("code", code),
                challenge.accountToken());
        return result.getJSONArray("recovery_codes");
    }

    public record Challenge(String accountToken, String authenticatorUri) {}
}
