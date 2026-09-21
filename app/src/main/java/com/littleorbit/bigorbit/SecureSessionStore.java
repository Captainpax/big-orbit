package com.littleorbit.bigorbit;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** AES-GCM storage for the device credential and short administrator token. */
public final class SecureSessionStore {
    private static final String ALIAS = "big-orbit-session-storage-v1";
    private static final String KEY = "sealed_admin_state";
    private final SharedPreferences preferences;

    public SecureSessionStore(Context context) {
        preferences = context.getSharedPreferences("big_orbit_private", Context.MODE_PRIVATE);
    }

    public synchronized State read() {
        String sealed = preferences.getString(KEY, null);
        if (sealed == null) return null;
        try {
            JSONObject value = new JSONObject(open(sealed));
            return new State(
                    value.getString("device_id"),
                    value.getString("device_credential"),
                    value.optString("access_token", ""));
        } catch (Exception invalid) {
            clear();
            return null;
        }
    }

    public synchronized void save(State state) throws Exception {
        JSONObject value = new JSONObject()
                .put("device_id", state.deviceId())
                .put("device_credential", state.deviceCredential())
                .put("access_token", state.accessToken());
        preferences.edit().putString(KEY, seal(value.toString())).apply();
    }

    public synchronized void clear() {
        preferences.edit().remove(KEY).apply();
    }

    public synchronized void clearAccessToken() {
        State current = read();
        if (current == null) return;
        try {
            save(new State(current.deviceId(), current.deviceCredential(), ""));
        } catch (Exception failure) {
            clear();
        }
    }

    private String seal(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, storageKey());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "."
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private String open(String value) throws Exception {
        String[] pieces = value.split("\\.", 2);
        if (pieces.length != 2) throw new IllegalArgumentException("Invalid sealed state");
        byte[] iv = Base64.decode(pieces[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(pieces[1], Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, storageKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private SecretKey storageKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        KeyStore.Entry existing = store.getEntry(ALIAS, null);
        if (existing instanceof KeyStore.SecretKeyEntry entry) return entry.getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    public record State(String deviceId, String deviceCredential, String accessToken) {}
}
