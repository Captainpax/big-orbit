package com.littleorbit.bigorbit;

import java.io.IOException;
import java.time.Duration;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Small synchronous JSON client used only from bounded background executors. */
public final class ApiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final String baseUrl;

    public ApiClient() {
        if (!EndpointPolicy.trusted(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)) {
            throw new IllegalStateException("Untrusted Big Orbit API endpoint");
        }
        baseUrl = BuildConfig.API_BASE_URL;
        client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .writeTimeout(Duration.ofSeconds(20))
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    public JSONObject getObject(String path, String token)
            throws IOException, ApiException, JSONException {
        return new JSONObject(execute("GET", path, null, token));
    }

    public JSONArray getArray(String path, String token)
            throws IOException, ApiException, JSONException {
        return new JSONArray(execute("GET", path, null, token));
    }

    public JSONObject post(String path, JSONObject body, String token)
            throws IOException, ApiException, JSONException {
        return new JSONObject(execute("POST", path, body, token));
    }

    public JSONObject delete(String path, JSONObject body, String token)
            throws IOException, ApiException, JSONException {
        return new JSONObject(execute("DELETE", path, body, token));
    }

    private String execute(String method, String path, JSONObject body, String token)
            throws IOException, ApiException {
        if (!path.startsWith("/v1/") && !path.startsWith("/v2/")) {
            throw new IllegalArgumentException("Big Orbit path is not versioned");
        }
        Request.Builder request = new Request.Builder()
                .url(baseUrl + path)
                .header("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }
        RequestBody requestBody = body == null ? null : RequestBody.create(body.toString(), JSON);
        request.method(method, requestBody);
        try (Response response = client.newCall(request.build()).execute()) {
            if (response.isRedirect()) {
                throw new ApiException(response.code());
            }
            if (!response.isSuccessful()) {
                throw new ApiException(response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return "{}";
            }
            String value = responseBody.string();
            if (value.length() > 1_048_576) {
                throw new IOException("Big Orbit response exceeded its bound");
            }
            return value;
        }
    }
}
