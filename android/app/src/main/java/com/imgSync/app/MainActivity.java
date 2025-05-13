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
        initCapacitorPreferences();
    }

    private void initCapacitorPreferences() {
        try {
            SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
                
            prefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
                // Reaccionar a cambios en cualquiera de las dos claves
                if ("widget_data".equals(key) || "last_post".equals(key)) {
                    Log.d("PREF_CHANGE", "Datos cambiados en clave: " + key);
                    triggerWidgetUpdate();
                }
            });
        } catch (Exception e) {
            Log.e("MAIN_ACTIVITY", "Error en initCapacitorPreferences", e);
        }
    }

    private void triggerWidgetUpdate() {
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, ImgSyncWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        if (widgetIds.length > 0) {
            Log.d("WIDGET_UPDATE", "Enviando actualización para " + widgetIds.length + " widgets");
            Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            updateIntent.setComponent(widgetComponent);
            sendBroadcast(updateIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("MAIN_ACTIVITY", "Aplicación en primer plano - Forzando actualización de widget");
        triggerWidgetUpdate();
    }
}
