package com.imgSync.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;

public class ImgSyncWidgetProvider extends AppWidgetProvider {
    private static final String PREFS_NAME = "CapacitorStorage";
    private static final String WIDGET_DATA_KEY = "widget_data";
    private static final String LAST_POST_KEY = "last_post";
    private static final long ROTATION_INTERVAL_MS = 5000; // 5 segundos
    private static final long UPDATE_INTERVAL_MINUTES = 15; // Mínimo permitido por WorkManager

    private final Executor executor = Executors.newSingleThreadExecutor();
    private static Handler rotationHandler;
    private static Runnable rotationRunnable;
    private static int currentImageIndex = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("WIDGET_LIFECYCLE", "onUpdate triggered");

        // Detener rotación anterior si existe
        stopImageRotation();

        // Iniciar rotación automática
        startImageRotation(context, appWidgetManager, appWidgetIds);

        // Actualización inicial
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, true);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d("WIDGET_LIFECYCLE", "Widget enabled");
        schedulePeriodicUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d("WIDGET_LIFECYCLE", "Widget disabled");
        stopImageRotation();
    }

    private void schedulePeriodicUpdate(Context context) {
        PeriodicWorkRequest widgetUpdateRequest =
            new PeriodicWorkRequest.Builder(
                WidgetUpdateWorker.class,
                UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "widgetUpdateWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                widgetUpdateRequest);

        Log.d("WIDGET_SCHEDULER", "Scheduled background update every " + UPDATE_INTERVAL_MINUTES + " minutes");
    }

    private void startImageRotation(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        rotationHandler = new Handler(Looper.getMainLooper());

        rotationRunnable = new Runnable() {
            @Override
            public void run() {
                List<String> imageUrls = getAllImageUrlsFromPreferences(context);

                if (!imageUrls.isEmpty()) {
                    currentImageIndex = (currentImageIndex + 1) % imageUrls.size();
                    String nextImageUrl = imageUrls.get(currentImageIndex);

                    for (int appWidgetId : appWidgetIds) {
                        updateWidgetImage(context, appWidgetManager, appWidgetId, nextImageUrl);
                    }
                }

                rotationHandler.postDelayed(this, ROTATION_INTERVAL_MS);
            }
        };

        rotationHandler.postDelayed(rotationRunnable, ROTATION_INTERVAL_MS);
    }

    private void stopImageRotation() {
        if (rotationHandler != null && rotationRunnable != null) {
            rotationHandler.removeCallbacks(rotationRunnable);
            rotationHandler = null;
            rotationRunnable = null;
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager,
                            int appWidgetId, boolean initialLoad) {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String jsonData = prefs.getString(WIDGET_DATA_KEY,
                                    prefs.getString(LAST_POST_KEY, "{}"));

                JSONObject data = new JSONObject(jsonData);
                String description = data.optString("description", "No description available");
                String imageUrl = data.optString("imageUrl", "");

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                views.setTextViewText(R.id.widget_description, description);

                if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                    loadImageWithGlide(context, appWidgetId, views, imageUrl, initialLoad);
                }

                appWidgetManager.updateAppWidget(appWidgetId, views);

            } catch (Exception e) {
                Log.e("WIDGET_ERROR", "Update failed: " + e.getMessage(), e);
            }
        });
    }

    private void updateWidgetImage(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId, String imageUrl) {
        executor.execute(() -> {
            try {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

                // Mantener la descripción actual
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String jsonData = prefs.getString(WIDGET_DATA_KEY,
                                    prefs.getString(LAST_POST_KEY, "{}"));
                JSONObject data = new JSONObject(jsonData);
                String description = data.optString("description", "No description available");
                views.setTextViewText(R.id.widget_description, description);

                loadImageWithGlide(context, appWidgetId, views, imageUrl, false);

                // Actualización parcial para evitar parpadeo
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);

            } catch (Exception e) {
                Log.e("WIDGET_ERROR", "Image rotation failed: " + e.getMessage(), e);
            }
        });
    }

    private void loadImageWithGlide(Context context, int appWidgetId,
                                  RemoteViews views, String imageUrl, boolean initialLoad) {
        try {
            AppWidgetTarget target = new AppWidgetTarget(context, R.id.widget_image, views, appWidgetId);

            Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(imageUrl)
                .apply(new RequestOptions()
                    .dontAnimate() // Desactivar animación por defecto
                    .skipMemoryCache(false) // Usar cache de memoria
                    .diskCacheStrategy(DiskCacheStrategy.ALL)) // Cachear en disco
                .transition(BitmapTransitionOptions.withCrossFade(500)) // Transición suave de 500ms
                .into(target);
                
        } catch (Exception e) {
            Log.e("GLIDE_ERROR", "Image load failed: " + e.getMessage(), e);
        }
    }

    private List<String> getAllImageUrlsFromPreferences(Context context) {
        List<String> imageUrls = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                if (entry.getKey().startsWith("widget_data") || entry.getKey().startsWith("last_post")) {
                    JSONObject data = new JSONObject((String) entry.getValue());
                    if (data.has("imageUrl")) {
                        String url = data.getString("imageUrl");
                        if (url.startsWith("http")) {
                            imageUrls.add(url);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("WIDGET_DATA", "Error parsing: " + entry.getKey(), e);
            }
        }

        return imageUrls;
    }
}
