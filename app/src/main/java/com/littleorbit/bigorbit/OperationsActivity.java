package com.littleorbit.bigorbit;

import android.os.Bundle;
import android.view.View;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Encrypted backup evidence and allowlisted host-operation requests. */
public final class OperationsActivity extends ConsoleActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        configure(
                R.string.operations,
                "Backup and restore evidence is content-free; jobs can only use fixed operation types.",
                R.id.nav_operations);
        primaryAction.setText(R.string.backup_now);
        secondaryAction.setText(R.string.test_restore);
        primaryAction.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.VISIBLE);
        primaryAction.setOnClickListener(ignored -> queue("backup"));
        secondaryAction.setOnClickListener(ignored -> queue("test_restore"));
        refresh();
    }

    @Override
    protected void refresh() {
        loadObject("/v2/admin/backups", this::render);
    }

    private void render(JSONObject response) {
        JSONArray items = response.optJSONArray("items");
        if (items == null) items = new JSONArray();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) continue;
            String body = "Destination: " + item.optString("destination", "unknown")
                    + "\nManifest: " + shortHash(item.optString("manifest_sha256", "pending"));
            addCard(
                    item.optString("status", "run").toUpperCase(Locale.ROOT),
                    item.optString("kind", "backup").replace('_', ' '),
                    body,
                    item.optString("created_at", ""));
        }
        primaryAction.setEnabled(true);
        secondaryAction.setEnabled(true);
        finishCards(items.length() == 0, R.string.empty_inbox);
    }

    private void queue(String kind) {
        try {
            JSONObject payload = new JSONObject()
                    .put("operation_id", UUID.randomUUID().toString())
                    .put("kind", kind);
            post("/v2/admin/jobs", payload, this::refresh);
        } catch (org.json.JSONException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private static String shortHash(String value) {
        return value.length() > 16 ? value.substring(0, 16) + "…" : value;
    }
}
