package com.laxy.ecgrate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.laxy.ecgrate.MainActivity
import com.laxy.ecgrate.R
import com.laxy.ecgrate.global.RateTask
import com.laxy.ecgrate.global.RateTask.selectedData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

/**
 * Implementation of App Widget functionality.
 */
class RateWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH: String = "refresh"
        const val ACTION_CONFIG: String = "config"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        intent?.let {
            when {
                ACTION_REFRESH == it.action -> {
                    context?.let {
                        RateTask.initSp(it)
                    }
                    RateTask.flush()
                    context?.sendBroadcast(Intent().apply {
                        setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    })
                }

                Intent.ACTION_SCREEN_ON == it.action -> {
                    context?.let {
                        RateTask.initSp(it)
                    }
                    RateTask.schedule()
                    context?.sendBroadcast(Intent().apply {
                        setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    })
                }

                ACTION_CONFIG == it.action -> {
                    context?.startActivity(Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }

                it.action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) -> {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisAppWidget = ComponentName(context!!.packageName, RateWidget::class.java.getName())
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                    appWidgetManager.updateAppWidget(appWidgetIds, RemoteViews(context.packageName, R.layout.rate_widget))
                    updateAppWidget(context, appWidgetManager, appWidgetIds.first())
                }

                else -> {}
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.rate_widget)
    views.setOnClickPendingIntent(R.id.root, getPendingSelfIntent(context, RateWidget.ACTION_CONFIG))
    var currencyName: String? = null
    var currencySymbol: String? = null
    selectedData.observeForever { body ->
        if (body == null) {
            return@observeForever
        }
        views.setViewVisibility(R.id.content, View.VISIBLE)
        views.setViewVisibility(R.id.appwidget_text, View.GONE)
        if (currencyName != body.ccyNbrEng) {
            currencyName = body.ccyNbrEng
            currencySymbol = getCurrencySymbol(body.ccyNbrEng.replace(body.ccyNbr, "").trim())
        }
        views.setTextViewText(R.id.currencyName, "100${currencySymbol} → CNY")
        views.setTextViewText(R.id.rate, "${body.rthBid}")
        views.setTextViewText(R.id.time, "已更新 ${body.ratTim}")
        views.setOnClickPendingIntent(R.id.lastRefreshTime, getPendingSelfIntent(context, RateWidget.ACTION_REFRESH))
        appWidgetManager.updateAppWidget(appWidgetId, views)
        GlobalScope.launch {
            context.sendBroadcast(Intent().apply {
                setAction(RateWidget.ACTION_REFRESH)
            })
        }
    }
    if (currencyName == null) {
        views.setViewVisibility(R.id.content, View.GONE)
        views.setViewVisibility(R.id.appwidget_text, View.VISIBLE)
    }
    views.setOnClickPendingIntent(R.id.appwidget_text, getPendingSelfIntent(context, RateWidget.ACTION_CONFIG))
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getCurrencySymbol(symbolName: String): String {
    val locale = Locale.getDefault()
    val currency: Currency = Currency.getInstance(symbolName)
    return currency.getSymbol(locale)
}

private fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
    val intent = Intent(context, RateWidget::class.java)
    intent.setAction(action)
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}
