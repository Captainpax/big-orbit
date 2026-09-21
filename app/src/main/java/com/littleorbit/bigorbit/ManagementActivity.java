package com.littleorbit.bigorbit;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import java.util.Iterator;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Privacy-safe health, registration, account, and security administration. */
public final class ManagementActivity extends ConsoleActivity {
    private int renderedCards;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        configure(
                R.string.owner_controls_title,
                "Service health and account metadata only—never relationship content.",
                0);
        findViewById(R.id.bottomNavigation).setVisibility(View.GONE);
        primaryAction.setText(R.string.open_registration);
        secondaryAction.setText(R.string.pause_registration);
        primaryAction.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.VISIBLE);
        primaryAction.setOnClickListener(ignored -> confirmRegistration(true));
        secondaryAction.setOnClickListener(ignored -> confirmRegistration(false));
        refresh();
    }

    @Override
    protected void refresh() {
        renderedCards = 0;
        loadObject("/v2/admin/overview", this::renderOverview);
    }

    private void renderOverview(JSONObject response) {
        JSONObject values = response.optJSONObject("values");
        if (values == null) values = new JSONObject();
        JSONObject services = values.optJSONObject("services");
        if (services != null) {
            Iterator<String> names = services.keys();
            while (names.hasNext()) {
                String name = names.next();
                if ("scheduler_days_covered".equals(name)) continue;
                addCard(
                        "SERVICE HEALTH",
                        name.replace('_', ' '),
                        String.valueOf(services.opt(name)),
                        "Operational metadata");
                renderedCards++;
            }
        }
        String counts = values.optInt("accounts") + " accounts · "
                + values.optInt("active_couples") + " active relationship containers\n"
                + values.optInt("question_reports") + " open reports · "
                + values.optInt("deletion_jobs") + " scheduled erasures";
        addCard("PRIVATE OPERATIONS", "System totals", counts, "No content is available here");
        renderedCards++;
        appendObject("/v2/admin/accounts?limit=100", this::renderAccounts);
    }

    private void renderAccounts(JSONObject response) {
        JSONArray items = response.optJSONArray("items");
        if (items == null) items = new JSONArray();
        for (int index = 0; index < items.length(); index++) {
            JSONObject account = items.optJSONObject(index);
            if (account == null) continue;
            renderAccount(account);
        }
        appendObject("/v2/admin/security-events", this::renderSecurityEvents);
    }

    private void renderAccount(JSONObject account) {
        boolean admin = account.optBoolean("is_admin");
        boolean suspended = !account.isNull("suspended_at");
        boolean deleted = !account.isNull("deleted_at");
        String state = deleted ? "deleted" : suspended ? "suspended" : "active";
        String verified = account.isNull("verified_at") ? "unverified" : "verified";
        View card = addCard(
                admin ? "OWNER ACCOUNT" : "ACCOUNT METADATA",
                account.optString("display_name", "Account"),
                account.optString("email", "Email unavailable") + "\n" + verified,
                state + (admin ? " · protected owner" : " · tap to manage"));
        renderedCards++;
        if (!admin && !deleted) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setContentDescription("Manage " + account.optString("display_name", "account"));
            card.setOnClickListener(ignored -> manageAccount(account, suspended));
        }
    }

    private void renderSecurityEvents(JSONObject response) {
        JSONArray items = response.optJSONArray("items");
        if (items == null) items = new JSONArray();
        for (int index = 0; index < Math.min(items.length(), 20); index++) {
            JSONObject event = items.optJSONObject(index);
            if (event == null) continue;
            addCard(
                    "SECURITY EVENT",
                    event.optString("event_type", "security event").replace('_', ' '),
                    event.optString("outcome", "recorded").toUpperCase(Locale.ROOT),
                    event.optString("created_at", ""));
            renderedCards++;
        }
        primaryAction.setEnabled(true);
        secondaryAction.setEnabled(true);
        finishCards(renderedCards == 0, R.string.empty_inbox);
    }

    private void manageAccount(JSONObject account, boolean suspended) {
        int[] choice = {0};
        String stateAction = suspended ? "Restore account" : "Suspend account";
        new AlertDialog.Builder(this)
                .setTitle(account.optString("display_name", "Manage account"))
                .setSingleChoiceItems(
                        new String[]{stateAction, "Revoke every session"},
                        0,
                        (dialog, selected) -> choice[0] = selected)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Continue", (dialog, which) -> {
                    if (choice[0] == 0) updateAccount(account.optString("id"), !suspended);
                    else revokeSessions(account.optString("id"));
                })
                .show();
    }

    private void updateAccount(String accountId, boolean suspended) {
        try {
            patch(
                    "/v2/admin/accounts/" + accountId,
                    new JSONObject().put("suspended", suspended),
                    this::refresh);
        } catch (JSONException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private void revokeSessions(String accountId) {
        post(
                "/v2/admin/accounts/" + accountId + "/revoke-sessions",
                new JSONObject(),
                this::refresh);
    }

    private void confirmRegistration(boolean enabled) {
        String action = enabled ? "Open public registration?" : "Pause public registration?";
        new AlertDialog.Builder(this)
                .setTitle(action)
                .setMessage("This changes new-account creation immediately. Existing accounts are unaffected.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Confirm", (dialog, which) -> updateRegistration(enabled))
                .show();
    }

    private void updateRegistration(boolean enabled) {
        try {
            put(
                    "/v2/admin/registration",
                    new JSONObject().put("enabled", enabled),
                    this::refresh);
        } catch (JSONException invalid) {
            throw new IllegalStateException(invalid);
        }
    }
}
