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
import ru.vladigeras.weatherapp.MainActivity
import ru.vladigeras.weatherapp.R
import ru.vladigeras.weatherapp.util.WeatherCodeMapper

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
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
        val sizeCategory = when {
            widthDp < 240 -> WidgetSizeCategory.SMALL
            widthDp <= 310 -> WidgetSizeCategory.MEDIUM
            else -> WidgetSizeCategory.LARGE
        }
        updateWidgetWithSize(context, appWidgetManager, appWidgetId, sizeCategory, heightDp)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_weather)

        if (!WidgetPrefsManager.hasData(context)) {
            views.setTextViewText(R.id.widget_city, context.getString(R.string.widget_no_data))
            views.setTextViewText(R.id.widget_temp, "")
            views.setTextViewText(R.id.widget_feels_like, "")
            views.setViewVisibility(R.id.widget_icon, View.GONE)
        } else {
            val cityName = WidgetPrefsManager.getCityName(context) ?: ""
            val temperature = WidgetPrefsManager.getTemperature(context)
            val feelsLike = WidgetPrefsManager.getFeelsLike(context)
            val weatherCode = WidgetPrefsManager.getWeatherCode(context) ?: 0
            val isDay = WidgetPrefsManager.getIsDay(context) ?: 1

            views.setTextViewText(R.id.widget_city, cityName)

            if (temperature != null) {
                views.setTextViewText(R.id.widget_temp, temperature)
            } else {
                views.setTextViewText(R.id.widget_temp, "")
            }

            if (feelsLike != null) {
                val feelsLikeFormatted = "(${context.getString(R.string.feels_like)} $feelsLike)"
                views.setTextViewText(R.id.widget_feels_like, feelsLikeFormatted)
                views.setViewVisibility(R.id.widget_feels_like, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_feels_like, View.GONE)
            }

            val iconRes = getWeatherIcon(weatherCode, isDay)
            views.setImageViewResource(R.id.widget_icon, iconRes)
            views.setViewVisibility(R.id.widget_icon, View.VISIBLE)
        }

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

    private fun updateWidgetWithSize(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        sizeCategory: WidgetSizeCategory,
        heightDp: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_weather)

        val (citySize, tempSize, padding) = when (sizeCategory) {
            WidgetSizeCategory.SMALL -> Triple(10f, 16f, 8)
            WidgetSizeCategory.MEDIUM -> Triple(12f, 20f, 12)
            WidgetSizeCategory.LARGE -> Triple(14f, 24f, 16)
        }

        val isTall = heightDp > 90
        val topBottomPadding = (if (isTall) padding + 4 else padding).dpToPx(context)
        val sidePadding = padding.dpToPx(context)

        views.setTextViewTextSize(R.id.widget_city, TypedValue.COMPLEX_UNIT_SP, citySize)
        views.setTextViewTextSize(R.id.widget_temp, TypedValue.COMPLEX_UNIT_SP, tempSize)
        views.setViewPadding(R.id.root_layout, sidePadding, topBottomPadding, sidePadding, topBottomPadding)

        updateWidget(context, manager, widgetId)
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun getWeatherIcon(weatherCode: Int, isDay: Int): Int {
        return WeatherCodeMapper.getIconRes(weatherCode, isDay)
    }

    private enum class WidgetSizeCategory {
        SMALL, MEDIUM, LARGE
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeatherWidgetProvider::class.java)
            val widgetIds = manager.getAppWidgetIds(componentName)

            if (widgetIds.isNotEmpty()) {
                val provider = WeatherWidgetProvider()
                for (widgetId in widgetIds) {
                    provider.updateWidget(context, manager, widgetId)
                }
            }
        }
    }
}