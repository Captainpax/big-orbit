package com.littleorbit.bigorbit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

/** Enrolled key inventory with explicit local sign-out and current-key revocation. */
public final class DevicesActivity extends ConsoleActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        configure(
                R.string.devices,
                "Every administrator session is bound to one explicitly enrolled device key.",
                R.id.nav_devices);
        primaryAction.setText(R.string.sign_out);
        secondaryAction.setText(R.string.revoke_device);
        primaryAction.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.VISIBLE);
        primaryAction.setOnClickListener(ignored -> localSignOut());
        secondaryAction.setOnClickListener(ignored -> confirmRevocation());
        refresh();
    }

    @Override
    protected void refresh() {
        loadArray("/v2/admin/devices", this::render);
    }

    private void render(JSONArray items) {
        String current = sessions.currentDeviceId();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) continue;
            boolean thisDevice = Objects.equals(current, item.optString("id"));
            String body = "Key " + shortHash(item.optString("key_fingerprint"))
                    + "\nLast seen: " + item.optString("last_seen_at", "never");
            addCard(
                    thisDevice ? "THIS DEVICE" : "ENROLLED DEVICE",
                    item.optString("label", "Big Orbit device"),
                    body,
                    item.isNull("revoked_at") ? "Active" : "Revoked");
        }
        primaryAction.setEnabled(true);
        secondaryAction.setEnabled(current != null);
        finishCards(items.length() == 0, R.string.empty_inbox);
    }

    private void confirmRevocation() {
        new AlertDialog.Builder(this)
                .setTitle("Revoke this Big Orbit device?")
                .setMessage("Its credential and every bound session will stop working immediately.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Revoke", (dialog, which) -> revokeCurrent())
                .show();
    }

    private void revokeCurrent() {
        String id = sessions.currentDeviceId();
        if (id == null) return;
        delete("/v2/admin/devices/" + id, this::forgetRevokedDevice);
    }

    private void localSignOut() {
        sessions.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void forgetRevokedDevice() {
        sessions.forgetRevokedDevice();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private static String shortHash(String value) {
        return value.length() > 16 ? value.substring(0, 16) + "…" : value;
    }
}
