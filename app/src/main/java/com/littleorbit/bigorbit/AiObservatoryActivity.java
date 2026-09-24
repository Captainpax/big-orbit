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
                        + "\n" + item.optInt("knowledge_chunks") + " reviewed knowledge chunks"
                        + "\n" + item.optInt("upcoming_days") + " future days covered"
                        + "\nReserve: " + item.optInt("reserve_general") + " general · "
                        + item.optInt("reserve_intimacy") + " intimacy"
                        + "\nContext: " + item.optInt("context_sources") + " allowlisted source(s)";
                addCard(
                        "ACTIVE POLICY",
                        "Saturday " + item.optInt("learning_local_hour")
                                + ":00 · Sunday " + item.optInt("generation_local_hour")
                                + ":00 · " + item.optString("schedule_timezone"),
                        body,
                        "Context refreshed " + item.optString("context_last_fetched_at", "never"));
            } else if ("week_plan".equals(item.optString("type"))) {
                addCard(
                        item.optString("status", "planned").toUpperCase(Locale.ROOT),
                        item.optString("arc_title", "Weekly theme"),
                        weekSummary(item),
                        "Week of " + item.optString("week_start"));
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

    private static String weekSummary(JSONObject item) {
        StringBuilder value = new StringBuilder(item.optString("arc_summary"));
        JSONArray days = item.optJSONArray("days");
        if (days == null) return value.toString();
        for (int index = 0; index < days.length(); index++) {
            JSONObject day = days.optJSONObject(index);
            if (day == null) continue;
            value.append("\n• ").append(day.optString("date"))
                    .append(" — ").append(day.optString("title"));
            if (!day.optString("observance").isBlank()) {
                value.append(" · ").append(day.optString("observance"));
            }
        }
        return value.toString();
    }
}
