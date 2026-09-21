package com.littleorbit.bigorbit;

import android.os.Bundle;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only provenance, semantic-memory, and weekly run health. */
public final class AiObservatoryActivity extends ConsoleActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        configure(
                R.string.ai_observatory,
                "Prompt versions, model provenance, policy state, and failures—never private inputs.",
                R.id.nav_ai);
        refresh();
    }

    @Override
    protected void refresh() {
        loadObject("/v2/admin/ai/observatory", this::render);
    }

    private void render(JSONObject response) {
        JSONArray items = response.optJSONArray("items");
        if (items == null) items = new JSONArray();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) continue;
            if ("summary".equals(item.optString("type"))) {
                String body = "Policy v" + item.optString("active_policy_version", "none")
                        + "\n" + item.optInt("embedded_questions") + " semantic records"
                        + "\n" + item.optInt("upcoming_days") + " future days covered";
                addCard(
                        "ACTIVE POLICY",
                        "Saturday " + item.optInt("learning_hour_utc") + ":00 UTC · Sunday "
                                + item.optInt("generation_hour_utc") + ":00 UTC",
                        body,
                        "Local model · allowlisted context only");
            } else {
                addCard(
                        item.optString("status", "run").toUpperCase(Locale.ROOT),
                        item.optString("kind", "AI run"),
                        item.optJSONObject("summary") == null
                                ? "No summary" : item.optJSONObject("summary").toString(),
                        item.optString("started_at", ""));
            }
        }
        finishCards(items.length() == 0, R.string.empty_inbox);
    }
}
