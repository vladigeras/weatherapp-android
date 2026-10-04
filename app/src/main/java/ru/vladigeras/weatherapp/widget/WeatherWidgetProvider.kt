package ru.vladigeras.weatherapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.vladigeras.weatherapp.MainActivity
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        launchUpdate {
            for (widgetId in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 70)
                updateWidget(context, appWidgetManager, widgetId, widthDp, heightDp)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val widthDp = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val heightDp = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        launchUpdate { updateWidget(context, appWidgetManager, appWidgetId, widthDp, heightDp) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ru.vladigeras.weatherapp.UPDATE_WIDGETS_ON_LANGUAGE_CHANGE") {
            launchUpdate { updateAllWidgetsUnlocked(context) }
        }
    }

    private fun launchUpdate(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                updateMutex.withLock { withContext(Dispatchers.IO) { block() } }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private suspend fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int, widthDp: Int, heightDp: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_weather)

        val layoutMode = if (widthDp >= 260) LayoutMode.HORIZONTAL else LayoutMode.VERTICAL
        views.setViewVisibility(R.id.vertical_container, if (layoutMode == LayoutMode.VERTICAL) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.horizontal_container, if (layoutMode == LayoutMode.HORIZONTAL) View.VISIBLE else View.GONE)

        val data = WidgetPrefsManager.getData(context)
        if (data == null) {
            if (layoutMode == LayoutMode.VERTICAL) {
                views.setTextViewText(R.id.widget_city, context.getString(R.string.widget_no_data))
                views.setTextViewText(R.id.widget_description, "")
                views.setTextViewText(R.id.widget_temp, "")
            } else {
                views.setTextViewText(R.id.widget_city_horizontal, context.getString(R.string.widget_no_data))
                views.setTextViewText(R.id.widget_description_horizontal, "")
                views.setTextViewText(R.id.widget_temp_horizontal, "")
            }
            views.setViewVisibility(R.id.widget_icon, View.GONE)
        } else {
            val cityName = data.cityName
            val temperature = data.temperature
            val weatherCode = data.weatherCode ?: -1
            val isDay = data.isDay ?: -1

            val descriptionResId = WeatherCodeMapper.getWeatherCodeStringResId(weatherCode)
            val description = context.getString(descriptionResId)

            if (layoutMode == LayoutMode.VERTICAL) {
                views.setTextViewText(R.id.widget_city, cityName)
                views.setTextViewText(R.id.widget_description, description)
                if (temperature != null) {
                    views.setTextViewText(R.id.widget_temp, temperature)
                } else {
                    views.setTextViewText(R.id.widget_temp, "")
                }
            } else {
                views.setTextViewText(R.id.widget_city_horizontal, cityName)
                views.setTextViewText(R.id.widget_description_horizontal, description)
                if (temperature != null) {
                    views.setTextViewText(R.id.widget_temp_horizontal, temperature)
                } else {
                    views.setTextViewText(R.id.widget_temp_horizontal, "")
                }
            }

            val iconRes = getWeatherIcon(weatherCode, isDay)
            views.setImageViewResource(R.id.widget_icon, iconRes)
            views.setViewVisibility(R.id.widget_icon, View.VISIBLE)
        }

        val sizes = when {
            heightDp >= 140 && widthDp >= 320 -> floatArrayOf(22f, 18f, 36f, 16f)
            heightDp >= 140 && widthDp >= 260 -> floatArrayOf(18f, 15f, 28f, 14f)
            heightDp >= 140 && widthDp >= 200 -> floatArrayOf(14f, 12f, 22f, 12f)
            heightDp >= 140 -> floatArrayOf(12f, 10f, 18f, 10f)
            widthDp < 200 -> floatArrayOf(10f, 9f, 14f, 8f)
            widthDp < 260 -> floatArrayOf(11f, 10f, 16f, 10f)
            widthDp < 320 -> floatArrayOf(12f, 11f, 18f, 12f)
            else -> floatArrayOf(14f, 12f, 22f, 14f)
        }
        val citySize = sizes[0]
        val descriptionSize = sizes[1]
        val tempSize = sizes[2]
        val paddingInt = sizes[3].toInt()

        if (layoutMode == LayoutMode.VERTICAL) {
            views.setTextViewTextSize(R.id.widget_city, TypedValue.COMPLEX_UNIT_SP, citySize)
            views.setTextViewTextSize(R.id.widget_description, TypedValue.COMPLEX_UNIT_SP, descriptionSize)
            views.setTextViewTextSize(R.id.widget_temp, TypedValue.COMPLEX_UNIT_SP, tempSize)
        } else {
            views.setTextViewTextSize(R.id.widget_city_horizontal, TypedValue.COMPLEX_UNIT_SP, citySize)
            views.setTextViewTextSize(R.id.widget_description_horizontal, TypedValue.COMPLEX_UNIT_SP, descriptionSize)
            views.setTextViewTextSize(R.id.widget_temp_horizontal, TypedValue.COMPLEX_UNIT_SP, tempSize)
        }

        val topBottomPadding = paddingInt.dpToPx(context)
        val sidePadding = paddingInt.dpToPx(context)
        views.setViewPadding(R.id.root_layout, sidePadding, topBottomPadding, sidePadding, topBottomPadding)

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.root_layout, pendingIntent)

        manager.updateAppWidget(widgetId, views)
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun getWeatherIcon(weatherCode: Int, isDay: Int): Int {
        return WeatherCodeMapper.getIconRes(weatherCode, isDay)
    }

    private enum class LayoutMode {
        VERTICAL, HORIZONTAL
    }

    companion object {
        private val updateMutex = Mutex()

        suspend fun updateAllWidgets(context: Context) = updateMutex.withLock {
            withContext(Dispatchers.IO) { updateAllWidgetsUnlocked(context) }
        }

        private suspend fun updateAllWidgetsUnlocked(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeatherWidgetProvider::class.java)
            val widgetIds = manager.getAppWidgetIds(componentName)

            for (widgetId in widgetIds) {
                val options = manager.getAppWidgetOptions(widgetId)
                val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 70)
                WeatherWidgetProvider().updateWidget(context, manager, widgetId, widthDp, heightDp)
            }
        }
    }
}
