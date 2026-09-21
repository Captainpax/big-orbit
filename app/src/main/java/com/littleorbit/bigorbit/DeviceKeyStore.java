package com.littleorbit.bigorbit;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;

/** Non-exportable P-256 identity generated independently for this Big Orbit install. */
public final class DeviceKeyStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String ALIAS = "big-orbit-admin-device-v1";

    public String publicKeySpki() throws Exception {
        KeyPair pair = ensureKey();
        return Base64.encodeToString(pair.getPublic().getEncoded(), Base64.NO_WRAP);
    }

    public String sign(String purpose, String challengeId, String challenge) throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
        PrivateKey key = (PrivateKey) store.getKey(ALIAS, null);
        if (key == null) {
            ensureKey();
            key = (PrivateKey) store.getKey(ALIAS, null);
        }
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(key);
        signer.update(ChallengeMessage.canonical(purpose, challengeId, challenge)
                .getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP);
    }

    public void delete() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
        store.deleteEntry(ALIAS);
    }

    private KeyPair ensureKey() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEY_STORE);
        store.load(null);
        if (store.containsAlias(ALIAS)) {
            KeyStore.PrivateKeyEntry entry =
                    (KeyStore.PrivateKeyEntry) store.getEntry(ALIAS, null);
            return new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build();
        generator.initialize(spec);
        return generator.generateKeyPair();
    }
}
