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
import com.laxy.ecgrate.global.RateTask
import com.laxy.ecgrate.global.RateTask.selectedData
import java.util.Currency
import java.util.Locale

class RateWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_REFRESH: String = "refresh"
        const val ACTION_CONFIG: String = "config"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        intent?.let {
            when {
                ACTION_REFRESH == it.action -> {
                    context?.let { ctx ->
                        RateTask.initSp(ctx)
                        RateTask.flush()
                    }
                }

                Intent.ACTION_SCREEN_ON == it.action -> {
                    context?.let { ctx ->
                        RateTask.initSp(ctx)
                        val mgr = AppWidgetManager.getInstance(ctx)
                        val ids = mgr.getAppWidgetIds(ComponentName(ctx, RateWidget::class.java))
                        ids.forEach { id -> updateAppWidget(ctx, mgr, id) }
                    }
                    RateTask.schedule()
                }

                ACTION_CONFIG == it.action -> {
                    context?.startActivity(Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }

                it.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisAppWidget = ComponentName(context!!.packageName, RateWidget::class.java.name)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                    appWidgetIds.forEach { id -> updateAppWidget(context, appWidgetManager, id) }
                }

                else -> {}
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.rate_widget)
    views.setOnClickPendingIntent(R.id.root, getPendingSelfIntent(context, RateWidget.ACTION_CONFIG))

    // 读取 widget 当前高度（dp），决定显示哪些行
    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    // 货币名始终显示；更新时间在高度足够时显示
    val showTime = minHeightDp == 0 || minHeightDp >= 70

    val body = selectedData.value
    if (body != null) {
        val currencySymbol = try {
            getCurrencySymbol(body.ccyNbrEng.replace(body.ccyNbr, "").trim())
        } catch (e: Exception) {
            body.ccyNbr
        }
        views.setViewVisibility(R.id.content, View.VISIBLE)
        views.setViewVisibility(R.id.appwidget_text, View.GONE)
        views.setViewVisibility(R.id.currencyName, View.VISIBLE)
        views.setViewVisibility(R.id.lastRefreshTime, if (showTime) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.currencyName, "100${currencySymbol} → CNY")
        views.setTextViewText(R.id.rate, body.rthBid)
        views.setTextViewText(R.id.time, "↻  ${body.ratTim}")
        views.setOnClickPendingIntent(R.id.lastRefreshTime, getPendingSelfIntent(context, RateWidget.ACTION_REFRESH))
    } else {
        views.setViewVisibility(R.id.content, View.GONE)
        views.setViewVisibility(R.id.appwidget_text, View.VISIBLE)
    }
    views.setOnClickPendingIntent(R.id.appwidget_text, getPendingSelfIntent(context, RateWidget.ACTION_CONFIG))
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getCurrencySymbol(symbolName: String): String {
    return Currency.getInstance(symbolName).getSymbol(Locale.getDefault())
}

private fun getPendingSelfIntent(context: Context?, action: String?): PendingIntent {
    val intent = Intent(context, RateWidget::class.java)
    intent.action = action
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}
