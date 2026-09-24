package com.littleorbit.bigorbit;

import android.os.Bundle;
import android.view.View;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Thresholded ratings and sanitized review themes for global questions. */
public final class QuizIntelligenceActivity extends ConsoleActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        configure(
                R.string.quiz_intelligence,
                "K-anonymous ratings guide Saturday learning; no answers or couple identities appear here.",
                R.id.nav_quizzes);
        primaryAction.setText(R.string.learn_now);
        secondaryAction.setText(R.string.regenerate_week);
        primaryAction.setVisibility(View.VISIBLE);
        secondaryAction.setVisibility(View.VISIBLE);
        primaryAction.setOnClickListener(ignored -> queue("learn_quizzes", currentMonday()));
        secondaryAction.setOnClickListener(
                ignored -> queue("regenerate_quizzes", nextMonday()));
        refresh();
    }

    @Override
    protected void refresh() {
        loadArray("/v2/admin/quiz-intelligence?weeks=8", this::render);
    }

    private void render(JSONArray items) {
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) continue;
            String metrics = item.optInt("rating_count") + " ratings · "
                    + String.format(java.util.Locale.US, "%.1f", item.optDouble("average_stars"))
                    + " / 5";
            String themes = "Tags: " + item.optJSONObject("tag_counts")
                    + reviewSummary(item.optJSONArray("sanitized_reviews"));
            addCard(
                    item.optString("category", "QUESTION").toUpperCase(java.util.Locale.ROOT),
                    item.optString("prompt", "Question"),
                    themes,
                    metrics + " · week " + item.optString("week_start"));
        }
        primaryAction.setEnabled(true);
        secondaryAction.setEnabled(true);
        finishCards(items.length() == 0, R.string.empty_inbox);
    }

    private static String reviewSummary(JSONArray reviews) {
        if (reviews == null || reviews.length() == 0) return "";
        StringBuilder value = new StringBuilder("\nConsented review themes:");
        for (int index = 0; index < Math.min(reviews.length(), 3); index++) {
            value.append("\n• ").append(reviews.optString(index));
        }
        return value.toString();
    }

    private void queue(String kind, LocalDate week) {
        try {
            JSONObject payload = new JSONObject()
                    .put("operation_id", UUID.randomUUID().toString())
                    .put("kind", kind)
                    .put("target_week", week.toString());
            post("/v2/admin/jobs", payload, this::refresh);
        } catch (org.json.JSONException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private static LocalDate currentMonday() {
        return LocalDate.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static LocalDate nextMonday() {
        return LocalDate.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }
}
