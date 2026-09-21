package com.littleorbit.bigorbit;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.time.Duration;

/** Schedules first-party, content-free operations polling. */
public final class BigOrbitApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                AlertPollWorker.class, Duration.ofMinutes(15))
                .setConstraints(constraints)
                .setInitialDelay(Duration.ofMinutes(2))
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "big-orbit-alert-poll",
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }
}
