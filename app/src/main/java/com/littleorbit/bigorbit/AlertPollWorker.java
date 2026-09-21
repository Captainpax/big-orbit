package com.littleorbit.bigorbit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;

/** First-party polling for content-free owner alerts; no hosted push address exists. */
public final class AlertPollWorker extends Worker {
    private static final String CHANNEL = "big-orbit-operations";

    public AlertPollWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        AdminSessionCoordinator sessions = new AdminSessionCoordinator(getApplicationContext());
        if (!sessions.isEnrolled() || !notificationsAllowed()) return Result.success();
        try {
            JSONArray alerts = sessions.authorizedArray(
                    "/v2/admin/alerts?acknowledged=false");
            createChannel();
            for (int index = 0; index < alerts.length(); index++) {
                JSONObject alert = alerts.optJSONObject(index);
                if (alert != null) notify(alert);
            }
            return Result.success();
        } catch (Exception failure) {
            return getRunAttemptCount() < 3 ? Result.retry() : Result.success();
        }
    }

    @SuppressLint("MissingPermission")
    private void notify(JSONObject alert) {
        if (!notificationsAllowed()) return;
        String id = alert.optString("id", "operations");
        Intent intent = new Intent(getApplicationContext(), DashboardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                getApplicationContext(), id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification publicVersion = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL)
                .setSmallIcon(R.drawable.ic_orbit)
                .setContentTitle("Big Orbit")
                .setContentText("An owner action needs attention.")
                .build();
        Notification notification = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL)
                .setSmallIcon(R.drawable.ic_orbit)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.lavender))
                .setContentTitle(alert.optString("title", "Big Orbit needs attention"))
                .setContentText(alert.optString("summary", "Open the action inbox."))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(alert.optString("summary", "Open the action inbox.")))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .build();
        try {
            NotificationManagerCompat.from(getApplicationContext())
                    .notify(id.hashCode(), notification);
        } catch (SecurityException ignored) {
            // Permission can be revoked between the explicit check and this call.
        }
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL,
                getApplicationContext().getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(getApplicationContext()
                .getString(R.string.notification_channel_description));
        NotificationManager manager = getApplicationContext()
                .getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private boolean notificationsAllowed() {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
