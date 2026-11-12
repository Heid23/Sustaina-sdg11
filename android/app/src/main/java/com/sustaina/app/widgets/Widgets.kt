package com.sustaina.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.sustaina.app.MainActivity
import com.sustaina.app.R

/**
 * One widget = one key. The website works out the numbers and text (so the widgets
 * always match the app); this class only draws them. Nothing here is invented.
 */
abstract class BaseWidget(
    private val key: String, private val title: String, private val bg: Int,
    private val textColor: String, private val mutedColor: String
) : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { ids.forEach { draw(c, m, it) } }
    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: Bundle) { draw(c, m, id) }

    private fun draw(c: Context, m: AppWidgetManager, id: Int) {
        val v = RemoteViews(c.packageName, R.layout.widget_generic)
        val t = Color.parseColor(textColor)
        val mu = Color.parseColor(mutedColor)
        v.setInt(R.id.root, "setBackgroundResource", bg)
        v.setTextViewText(R.id.wtitle, "SUSTAINA  \u00b7  $title")
        v.setTextColor(R.id.wtitle, mu)
        val d = WidgetData.get(c, key)
        val h = m.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val lines = listOf(R.id.l1, R.id.l2, R.id.l3)
        if (d == null) {
            v.setTextViewText(R.id.big, "Open SUSTAINA")
            v.setTextViewText(R.id.sub, "Open the app once to load your records.")
            v.setViewVisibility(R.id.bar, View.GONE)
            lines.forEach { v.setViewVisibility(it, View.GONE) }
        } else {
            val big = d.optString("big", "")
            v.setTextViewText(R.id.big, big)
            v.setViewVisibility(R.id.big, if (big.isEmpty()) View.GONE else View.VISIBLE)
            v.setTextViewText(R.id.sub, d.optString("sub", ""))
            val p = d.optInt("p", -1)
            if (p >= 0 && h >= 100) { v.setViewVisibility(R.id.bar, View.VISIBLE); v.setProgressBar(R.id.bar, 100, p.coerceIn(0, 100), false) }
            else v.setViewVisibility(R.id.bar, View.GONE)
            val arr = d.optJSONArray("lines")
            val room = if (h >= 230) 3 else if (h >= 150) 2 else if (h >= 110) 1 else 0
            for (i in 0 until 3) {
                val txt = if (arr != null && i < arr.length()) arr.optString(i) else ""
                if (i < room && txt.isNotEmpty()) { v.setViewVisibility(lines[i], View.VISIBLE); v.setTextViewText(lines[i], txt); v.setTextColor(lines[i], t) }
                else v.setViewVisibility(lines[i], View.GONE)
            }
        }
        v.setTextColor(R.id.big, t); v.setTextColor(R.id.sub, mu)
        val open = Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, key.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        m.updateAppWidget(id, v)
    }
}

class WHabit : BaseWidget("habit", "Daily Habit Tracker", R.drawable.bg_habit, "#14532D", "#4B6B57")
class WStreak : BaseWidget("streak", "Habit Streak Tracker", R.drawable.bg_streak, "#7C2D12", "#8A5A3C")
class WJournal : BaseWidget("journal", "Daily Activity Journal", R.drawable.bg_journal, "#14532D", "#4B6B57")
class WWater : BaseWidget("water", "Water-Saving Tracker", R.drawable.bg_water, "#0C4A6E", "#3F6B82")
class WWaste : BaseWidget("waste", "Waste Reduction Tracker", R.drawable.bg_waste, "#14532D", "#4B6B57")
class WEnergy : BaseWidget("energy", "Energy-Saving Tracker", R.drawable.bg_energy, "#4D3A05", "#6B5A2A")
class WReflect : BaseWidget("reflect", "Daily Reflection", R.drawable.bg_reflect, "#14532D", "#4B6B57")
class WGoals : BaseWidget("goals", "Personal Goals Tracker", R.drawable.bg_goals, "#14532D", "#4B6B57")
class WCal : BaseWidget("cal", "Habit Calendar", R.drawable.bg_cal, "#14532D", "#4B6B57")
class WTmrw : BaseWidget("tmrw", "Tomorrow's Eco Action", R.drawable.bg_tmrw, "#14532D", "#4B6B57")
class WSum : BaseWidget("sum", "Daily Impact", R.drawable.bg_sum, "#14532D", "#4B6B57")
