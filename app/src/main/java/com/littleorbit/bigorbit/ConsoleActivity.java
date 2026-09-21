package com.littleorbit.bigorbit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/** Shared accessible shell for every private operations destination. */
public abstract class ConsoleActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    protected AdminSessionCoordinator sessions;
    protected LinearLayout content;
    protected MaterialButton primaryAction;
    protected MaterialButton secondaryAction;
    private View loading;
    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        sessions = new AdminSessionCoordinator(this);
        if (!sessions.isEnrolled()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_console);
        content = findViewById(R.id.contentContainer);
        primaryAction = findViewById(R.id.primaryAction);
        secondaryAction = findViewById(R.id.secondaryAction);
        loading = findViewById(R.id.loadingProgress);
        status = findViewById(R.id.screenStatus);
        findViewById(R.id.refreshButton).setOnClickListener(ignored -> refresh());
        configureNavigation((BottomNavigationView) findViewById(R.id.bottomNavigation));
    }

    protected final void configure(
            @StringRes int title, String subtitle, int selectedNavigation) {
        ((TextView) findViewById(R.id.screenTitle)).setText(title);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(subtitle);
        ((BottomNavigationView) findViewById(R.id.bottomNavigation))
                .setSelectedItemId(selectedNavigation);
    }

    protected abstract void refresh();

    protected final void loadObject(String path, Consumer<JSONObject> renderer) {
        run(() -> sessions.authorizedObject(path), renderer);
    }

    protected final void loadArray(String path, Consumer<JSONArray> renderer) {
        run(() -> sessions.authorizedArray(path), renderer);
    }

    protected final void post(String path, JSONObject body, Runnable completed) {
        beginLoad();
        executor.execute(() -> {
            try {
                sessions.authorizedPost(path, body);
                main.post(completed);
            } catch (Exception failure) {
                main.post(this::showFailure);
            }
        });
    }

    protected final void delete(String path, Runnable completed) {
        beginLoad();
        executor.execute(() -> {
            try {
                sessions.authorizedDelete(path);
                main.post(completed);
            } catch (Exception failure) {
                main.post(this::showFailure);
            }
        });
    }

    protected final void addCard(String eyebrow, String title, String body, String meta) {
        View card = LayoutInflater.from(this).inflate(
                R.layout.item_console_card, content, false);
        ((TextView) card.findViewById(R.id.cardEyebrow)).setText(eyebrow);
        ((TextView) card.findViewById(R.id.cardTitle)).setText(title);
        ((TextView) card.findViewById(R.id.cardBody)).setText(body);
        ((TextView) card.findViewById(R.id.cardMeta)).setText(meta);
        content.addView(card);
    }

    protected final void finishCards(boolean empty, @StringRes int emptyText) {
        loading.setVisibility(View.GONE);
        status.setText(empty ? emptyText : 0);
        status.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    protected final void runBackground(Callable<Void> operation, Runnable completed) {
        executor.execute(() -> {
            try {
                operation.call();
                main.post(completed);
            } catch (Exception failure) {
                main.post(this::showFailure);
            }
        });
    }

    private <T> void run(Callable<T> request, Consumer<T> renderer) {
        beginLoad();
        executor.execute(() -> {
            try {
                T result = request.call();
                main.post(() -> renderer.accept(result));
            } catch (Exception failure) {
                main.post(this::showFailure);
            }
        });
    }

    private void beginLoad() {
        while (content.getChildCount() > 2) content.removeViewAt(2);
        loading.setVisibility(View.VISIBLE);
        status.setVisibility(View.VISIBLE);
        status.setText(R.string.loading);
        primaryAction.setEnabled(false);
        secondaryAction.setEnabled(false);
    }

    private void showFailure() {
        loading.setVisibility(View.GONE);
        status.setVisibility(View.VISIBLE);
        status.setText(R.string.network_error);
        primaryAction.setEnabled(true);
        secondaryAction.setEnabled(true);
    }

    private void configureNavigation(BottomNavigationView navigation) {
        navigation.setOnItemSelectedListener(item -> {
            Class<?> target = target(item.getItemId());
            if (target == null || target.equals(getClass())) return true;
            startActivity(new Intent(this, target));
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }

    private static Class<?> target(int id) {
        if (id == R.id.nav_inbox) return DashboardActivity.class;
        if (id == R.id.nav_quizzes) return QuizIntelligenceActivity.class;
        if (id == R.id.nav_ai) return AiObservatoryActivity.class;
        if (id == R.id.nav_operations) return OperationsActivity.class;
        if (id == R.id.nav_devices) return DevicesActivity.class;
        return null;
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
