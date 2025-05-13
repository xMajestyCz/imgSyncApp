package com.imgSync.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;

import org.json.JSONObject;

import java.util.Map;
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
    private static final String LAST_POST_KEY = "last_post"; // Nueva clave alternativa
    private static final long UPDATE_INTERVAL_MINUTES = 5;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("WIDGET_LIFECYCLE", "onUpdate triggered");
        schedulePeriodicUpdate(context);

        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d("WIDGET_LIFECYCLE", "Widget enabled");
        schedulePeriodicUpdate(context);
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
                ExistingPeriodicWorkPolicy.REPLACE, // Cambiado a REPLACE para asegurar actualizaciones
                widgetUpdateRequest);

        Log.d("WIDGET_SCHEDULER", "Scheduled next update in " + UPDATE_INTERVAL_MINUTES + " minutes");
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        executor.execute(() -> {
            try {
                // 1. Obtener preferencias compartidas
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

                // 2. Depuración: Mostrar todas las preferencias disponibles
                Map<String, ?> allPrefs = prefs.getAll();
                Log.d("WIDGET_DATA", "All preferences: " + allPrefs.toString());

                // 3. Intentar obtener datos de ambas claves posibles
                String jsonData = prefs.getString(WIDGET_DATA_KEY,
                                    prefs.getString(LAST_POST_KEY, "{}"));

                Log.d("WIDGET_DATA", "Raw JSON data: " + jsonData);

                // 4. Parsear JSON
                JSONObject data = new JSONObject(jsonData);
                String description = data.optString("description", "No description available");
                String imageUrl = data.optString("imageUrl", "");

                Log.d("WIDGET_DATA", "Description: " + description);
                Log.d("WIDGET_DATA", "Image URL: " + imageUrl);

                // 5. Preparar la vista del widget
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                views.setTextViewText(R.id.widget_description, description);

                // 6. Cargar imagen con manejo robusto de errores
                if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                    try {
                        AppWidgetTarget target = new AppWidgetTarget(
                            context.getApplicationContext(),
                            R.id.widget_image,
                            views,
                            appWidgetId);

                        Glide.with(context.getApplicationContext())
                            .asBitmap()
                            .load(imageUrl)
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                            Target<Bitmap> target, boolean isFirstResource) {
                                    Log.e("GLIDE_ERROR", "Load failed: " + (e != null ? e.getMessage() : "Unknown error"));
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Bitmap resource, Object model,
                                                             Target<Bitmap> target, DataSource dataSource,
                                                             boolean isFirstResource) {
                                    Log.d("GLIDE_SUCCESS", "Image loaded successfully");
                                    return false;
                                }
                            })
                            .into(target);

                        Log.d("WIDGET_IMAGE", "Image load initiated");
                    } catch (Exception e) {
                        Log.e("WIDGET_ERROR", "Glide initialization failed: " + e.getMessage());
                    }
                } else {
                    Log.e("WIDGET_ERROR", "Invalid image URL: " + imageUrl);
                }

                // 7. Actualizar el widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
                Log.d("WIDGET_UPDATE", "Widget updated successfully");

            } catch (Exception e) {
                Log.e("WIDGET_ERROR", "Update failed: " + e.getMessage(), e);
            }
        });
    }
}
