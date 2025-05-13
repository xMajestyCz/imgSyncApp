package com.imgSync.app;

import android.content.Context;
import android.content.ComponentName;
import android.appwidget.AppWidgetManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WidgetUpdateWorker extends Worker {
    public WidgetUpdateWorker(
        @NonNull Context context,
        @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, ImgSyncWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            
            // Actualizar el widget directamente sin crear nueva instancia
            ImgSyncWidgetProvider widgetProvider = new ImgSyncWidgetProvider();
            widgetProvider.onUpdate(context, appWidgetManager, appWidgetIds);
            
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}