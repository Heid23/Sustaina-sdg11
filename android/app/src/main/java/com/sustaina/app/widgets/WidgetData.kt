package com.sustaina.app.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.json.JSONObject

/** Stores the last numbers the website sent. Stays on the phone only. */
object WidgetData {
    private const val PREF = "sustaina_widgets"
    fun save(c: Context, json: String) { c.getSharedPreferences(PREF, 0).edit().putString("d", json).apply() }
    fun get(c: Context, key: String): JSONObject? {
        val raw = c.getSharedPreferences(PREF, 0).getString("d", null) ?: return null
        return try { JSONObject(raw).optJSONObject("w")?.optJSONObject(key) } catch (e: Exception) { null }
    }
    fun refreshAll(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        for (cls in ALL) {
            val ids = m.getAppWidgetIds(ComponentName(c, cls))
            if (ids.isNotEmpty()) c.sendBroadcast(Intent(c, cls).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
    val ALL: List<Class<*>> = listOf(WHabit::class.java, WStreak::class.java, WJournal::class.java, WWater::class.java,
        WWaste::class.java, WEnergy::class.java, WReflect::class.java, WGoals::class.java, WCal::class.java,
        WTmrw::class.java, WSum::class.java)
}
