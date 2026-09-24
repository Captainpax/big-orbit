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
    private static final String BOOTSTRAP_KEY = "sealed_bootstrap_state";
    private static final Object STATE_LOCK = new Object();
    private final SharedPreferences preferences;

    public SecureSessionStore(Context context) {
        preferences = context.getSharedPreferences("big_orbit_private", Context.MODE_PRIVATE);
    }

    public State read() {
        synchronized (STATE_LOCK) {
            return readLocked();
        }
    }

    private State readLocked() {
        String sealed = preferences.getString(KEY, null);
        if (sealed == null) return null;
        try {
            JSONObject value = new JSONObject(open(sealed));
            return new State(
                    value.getString("device_id"),
                    value.getString("device_credential"),
                    value.optString("access_token", ""));
        } catch (Exception invalid) {
            preferences.edit().remove(KEY).apply();
            return null;
        }
    }

    public void save(State state) throws Exception {
        synchronized (STATE_LOCK) {
            saveLocked(state);
        }
    }

    public BootstrapState readBootstrap() {
        synchronized (STATE_LOCK) {
            String sealed = preferences.getString(BOOTSTRAP_KEY, null);
            if (sealed == null) return null;
            try {
                JSONObject value = new JSONObject(open(sealed));
                return new BootstrapState(
                        value.getString("setup_token"), value.getString("device_id"));
            } catch (Exception invalid) {
                preferences.edit().remove(BOOTSTRAP_KEY).apply();
                return null;
            }
        }
    }

    public void saveBootstrap(BootstrapState state) throws Exception {
        synchronized (STATE_LOCK) {
            JSONObject value = new JSONObject()
                    .put("setup_token", state.setupToken())
                    .put("device_id", state.deviceId());
            preferences.edit().putString(BOOTSTRAP_KEY, seal(value.toString())).apply();
        }
    }

    public void clearBootstrap() {
        synchronized (STATE_LOCK) {
            preferences.edit().remove(BOOTSTRAP_KEY).apply();
        }
    }

    public boolean replaceIfCurrent(State expected, State updated) throws Exception {
        synchronized (STATE_LOCK) {
            State current = readLocked();
            if (!expected.equals(current)) return false;
            saveLocked(updated);
            return true;
        }
    }

    private void saveLocked(State state) throws Exception {
        JSONObject value = new JSONObject()
                .put("device_id", state.deviceId())
                .put("device_credential", state.deviceCredential())
                .put("access_token", state.accessToken());
        preferences.edit().putString(KEY, seal(value.toString())).apply();
    }

    public void clear() {
        synchronized (STATE_LOCK) {
            preferences.edit().remove(KEY).remove(BOOTSTRAP_KEY).apply();
        }
    }

    public void clearAccessToken() {
        synchronized (STATE_LOCK) {
            State current = readLocked();
            if (current == null) return;
            try {
                saveLocked(new State(current.deviceId(), current.deviceCredential(), ""));
            } catch (Exception failure) {
                preferences.edit().remove(KEY).apply();
            }
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

    /** Short-lived setup capability encrypted separately from approved credentials. */
    public record BootstrapState(String setupToken, String deviceId) {}
}
