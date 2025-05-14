    package com.imgSync.app;

    import android.content.ComponentName;
    import android.content.Context;
    import android.content.Intent;
    import android.appwidget.AppWidgetManager;
    import android.os.Bundle;
    import android.content.SharedPreferences;
    import android.util.Log;

    import com.getcapacitor.BridgeActivity;

    public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWidgetUpdateListener();
    }

    private void setupWidgetUpdateListener() {
        try {
        SharedPreferences prefs = getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);

        prefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if ("widget_data".equals(key)) {
            Log.d("WIDGET_UPDATE", "Widget data changed, triggering update");
            triggerWidgetUpdate();
            }
        });
        } catch (Exception e) {
        Log.e("MAIN_ACTIVITY", "Error setting up listener", e);
        }
    }

    private void triggerWidgetUpdate() {
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, ImgSyncWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        if (widgetIds.length > 0) {
        Intent updateIntent = new Intent(context, ImgSyncWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(updateIntent);
        }
        }
    }