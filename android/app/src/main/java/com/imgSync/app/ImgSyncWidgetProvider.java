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

    import org.json.JSONArray;
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
        private static final long ROTATION_INTERVAL_MS = 5000; // 5 segundos

        private Handler rotationHandler;
        private Runnable rotationRunnable;
        private int currentImageIndex = 0;

        @Override
        public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            Log.d("WIDGET", "onUpdate triggered");

            // Detener rotación anterior si existe
            stopImageRotation();

            // Iniciar nueva rotación
            startImageRotation(context, appWidgetManager, appWidgetIds);

            // Actualizar inmediatamente
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }

        private void startImageRotation(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            rotationHandler = new Handler(Looper.getMainLooper());

            rotationRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        String jsonData = prefs.getString(WIDGET_DATA_KEY, "{}");
                        JSONObject data = new JSONObject(jsonData);

                        if (data.has("posts")) {
                            JSONArray postsArray = data.getJSONArray("posts");

                            if (postsArray.length() > 0) {
                                currentImageIndex = (currentImageIndex + 1) % postsArray.length();
                                JSONObject currentPost = postsArray.getJSONObject(currentImageIndex);

                                String imageUrl = currentPost.getString("imageUrl");
                                String description = currentPost.getString("description");

                                for (int appWidgetId : appWidgetIds) {
                                    updateWidgetContent(context, appWidgetManager, appWidgetId, imageUrl, description);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("WIDGET_ROTATION", "Error during rotation", e);
                    } finally {
                        rotationHandler.postDelayed(this, ROTATION_INTERVAL_MS);
                    }
                }
            };

            rotationHandler.post(rotationRunnable);
        }

        private void stopImageRotation() {
            if (rotationHandler != null && rotationRunnable != null) {
                rotationHandler.removeCallbacks(rotationRunnable);
            }
        }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonData = prefs.getString(WIDGET_DATA_KEY, "{}");
        JSONObject data = new JSONObject(jsonData);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        if (data.has("posts")) {
            JSONArray postsArray = data.getJSONArray("posts");

            if (postsArray.length() > 0) {
            JSONObject firstPost = postsArray.getJSONObject(0);
            String imageUrl = firstPost.getString("imageUrl");
            String description = firstPost.getString("description");

            views.setTextViewText(R.id.widget_description, description);

            // Cargar imagen solo si hay URL válida
            if (imageUrl != null && !imageUrl.isEmpty()) {
                AppWidgetTarget target = new AppWidgetTarget(context, R.id.widget_image, views, appWidgetId);
                Glide.with(context.getApplicationContext())
                .asBitmap()
                .load(imageUrl)
                .apply(new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(target);
            }
            } else {
            views.setTextViewText(R.id.widget_description, "No hay imágenes disponibles");
            }
        } else {
            views.setTextViewText(R.id.widget_description, "No hay datos configurados");
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
        Log.e("WIDGET_UPDATE", "Error updating widget", e);
        }
    }

    private void updateWidgetContent(Context context, AppWidgetManager appWidgetManager,
                                    int appWidgetId, String imageUrl, String description) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.widget_description, description);

        AppWidgetTarget target = new AppWidgetTarget(context, R.id.widget_image, views, appWidgetId);

        try {
        Glide.with(context.getApplicationContext())
            .asBitmap()
            .load(imageUrl)
            .apply(new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL))
            .into(target);
        } catch (Exception e) {
        Log.e("GLIDE_ERROR", "Error loading image", e);
        views.setImageViewResource(R.id.widget_image, android.R.color.transparent);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

        @Override
        public void onDisabled(Context context) {
            super.onDisabled(context);
            stopImageRotation();
        }
    }
