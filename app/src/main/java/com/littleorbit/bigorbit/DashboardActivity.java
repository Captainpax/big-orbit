package com.littleorbit.bigorbit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Action-first home containing only privacy-safe operational metadata. */
public final class DashboardActivity extends ConsoleActivity {
    private final List<String> visibleAlertIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        configure(
                R.string.action_inbox,
                "The few things that need an owner decision, ordered by urgency.",
                R.id.nav_inbox);
        primaryAction.setText(R.string.acknowledge_alerts);
        secondaryAction.setText(R.string.owner_controls);
        primaryAction.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.VISIBLE);
        primaryAction.setOnClickListener(ignored -> acknowledge());
        secondaryAction.setOnClickListener(ignored ->
                startActivity(new Intent(this, ManagementActivity.class)));
        askNotificationPermission();
        refresh();
    }

    @Override
    protected void refresh() {
        visibleAlertIds.clear();
        loadObject("/v2/admin/action-inbox", this::render);
    }

    private void render(JSONObject response) {
        JSONArray items = response.optJSONArray("items");
        if (items == null) items = new JSONArray();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) continue;
            renderItem(item);
        }
        primaryAction.setEnabled(!visibleAlertIds.isEmpty());
        secondaryAction.setEnabled(true);
        finishCards(items.length() == 0, R.string.empty_inbox);
    }

    private void renderItem(JSONObject item) {
        String type = item.optString("type", "operation");
        if ("alert".equals(type)) {
            visibleAlertIds.add(item.optString("id"));
            addCard(
                    item.optString("severity", "INFO").toUpperCase(Locale.ROOT),
                    item.optString("title", "Operations alert"),
                    item.optString("summary", "Open the relevant destination to review."),
                    item.optString("created_at", ""));
        } else if ("question_report".equals(type)) {
            View card = addCard(
                    "GLOBAL QUESTION REPORT",
                    item.optString("prompt", "Question under review"),
                    item.optString("reason", "A safety report needs a decision."),
                    "Tap to resolve · " + item.optString("created_at", ""));
            String reportId = item.optString("id");
            String prompt = item.optString("prompt", "Question under review");
            if (!reportId.isBlank()) {
                card.setClickable(true);
                card.setFocusable(true);
                card.setContentDescription("Review reported question: " + prompt);
                card.setOnClickListener(ignored -> reviewReport(reportId, prompt));
            }
        } else {
            addCard(
                    "ALLOWLISTED JOB",
                    item.optString("kind", "Operations job"),
                    "Status: " + item.optString("status", "unknown"),
                    item.optString("created_at", ""));
        }
    }

    private void reviewReport(String reportId, String prompt) {
        int[] choice = {0};
        new AlertDialog.Builder(this)
                .setTitle("Resolve question report")
                .setMessage(prompt)
                .setSingleChoiceItems(
                        new String[]{"Keep the global question", "Disable it globally"},
                        0,
                        (dialog, selected) -> choice[0] = selected)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Resolve", (dialog, which) ->
                        resolveReport(reportId, choice[0] == 1))
                .show();
    }

    private void resolveReport(String reportId, boolean disableQuestion) {
        try {
            post(
                    "/v2/admin/question-reports/" + reportId + "/resolve",
                    new JSONObject().put("disable_question", disableQuestion),
                    this::refresh);
        } catch (org.json.JSONException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private void acknowledge() {
        if (visibleAlertIds.isEmpty()) return;
        JSONArray ids = new JSONArray();
        for (String id : visibleAlertIds) ids.put(id);
        try {
            post(
                    "/v2/admin/alerts/acknowledge",
                    new JSONObject().put("alert_ids", ids),
                    this::refresh);
        } catch (org.json.JSONException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1200);
        }
    }
}
