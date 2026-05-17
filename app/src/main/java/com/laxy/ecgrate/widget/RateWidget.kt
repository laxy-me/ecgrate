package com.laxy.ecgrate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.laxy.ecgrate.MainActivity
import com.laxy.ecgrate.R
import com.laxy.ecgrate.RateManager
import com.laxy.ecgrate.entity.CurrencyRate
import java.util.Currency
import java.util.Locale

class RateWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH = "refresh"
        const val ACTION_CONFIG = "config"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetId: Int, newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val ctx = context ?: return
        when (intent?.action) {
            ACTION_REFRESH -> {
                RateManager.flush()
            }
            Intent.ACTION_SCREEN_ON -> {
                RateManager.screenOn = true
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, RateWidget::class.java))
                ids.forEach { updateAppWidget(ctx, mgr, it) }
            }
            ACTION_CONFIG -> {
                ctx.startActivity(Intent(ctx, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx.packageName, RateWidget::class.java.name))
                ids.forEach { updateAppWidget(ctx, mgr, it) }
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.rate_widget)
    views.setOnClickPendingIntent(R.id.root, pendingIntent(context, RateWidget.ACTION_CONFIG))

    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    val showTime = minHeightDp == 0 || minHeightDp >= 70

    val client = (context.applicationContext as? com.laxy.ecgrate.EcgrateApp)?.rateClient
    val body: CurrencyRate.Body? = client?.selectedBody()

    if (body != null) {
        val symbol = try {
            val code = body.ccyNbrEng.replace(body.ccyNbr, "").trim()
            Currency.getInstance(code).getSymbol(Locale.getDefault())
        } catch (e: Exception) { body.ccyNbr }

        views.setViewVisibility(R.id.content, View.VISIBLE)
        views.setViewVisibility(R.id.appwidget_text, View.GONE)
        views.setViewVisibility(R.id.currencyName, View.VISIBLE)
        views.setViewVisibility(R.id.lastRefreshTime, if (showTime) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.currencyName, "100${symbol} → CNY")
        views.setTextViewText(R.id.rate, body.rthOfr)
        views.setTextViewText(R.id.time, "↻  ${body.ratTim}")
        views.setOnClickPendingIntent(R.id.lastRefreshTime, pendingIntent(context, RateWidget.ACTION_REFRESH))
    } else {
        views.setViewVisibility(R.id.content, View.GONE)
        views.setViewVisibility(R.id.appwidget_text, View.VISIBLE)
        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent(context, RateWidget.ACTION_CONFIG))
    }
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun pendingIntent(context: Context?, action: String?): PendingIntent {
    val intent = Intent(context, RateWidget::class.java).also { it.action = action }
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}
