package com.sustaina.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.sustaina.app.MainActivity
import com.sustaina.app.QuickEntryActivity
import com.sustaina.app.R
import org.json.JSONObject

class Btn(val label: String, val act: String)

/**
 * One widget = one key. The website works out numbers and text; this class draws them and
 * turns taps into queued actions (ActionStore) that the website applies to the real records.
 */
abstract class BaseWidget(
    private val key: String, private val title: String, private val bg: Int,
    private val textColor: String, private val mutedColor: String,
    private val btns: List<Btn> = emptyList()
) : AppWidgetProvider() {
    companion object {
        const val ACT_ROW = "com.sustaina.app.ROW"
        const val ACT_BTN = "com.sustaina.app.BTN"
    }

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { ids.forEach { draw(c, m, it) } }
    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: Bundle) { draw(c, m, id) }

    override fun onReceive(c: Context, i: Intent) {
        when (i.action) {
            ACT_ROW -> row(c, i.getIntExtra("i", -1))
            ACT_BTN -> btn(c, i.getStringExtra("a") ?: "")
            else -> super.onReceive(c, i)
        }
    }

    private fun row(c: Context, idx: Int) {
        val item = WidgetData.get(c, key)?.optJSONArray("items")?.optJSONObject(idx) ?: return
        val k = item.opt("k")?.toString() ?: ""
        if (k.isEmpty()) return
        val on = !item.optBoolean("on", false)
        WidgetData.setItem(c, key, idx, on)
        ActionStore.add(c, JSONObject().put("t", "hb").put("k", k).put("on", on).put("d", WidgetData.today()))
        WidgetData.refreshAll(c)
    }

    private fun btn(c: Context, a: String) {
        val d = WidgetData.get(c, key) ?: return
        if (a == "tmn") {
            WidgetData.nextIdea(c, key)
        } else if (a == "tmd" || a == "tmg") {
            val k = d.opt("k")?.toString() ?: ""
            if (k.isNotEmpty()) {
                ActionStore.add(c, JSONObject().put("t", a).put("k", k).put("d", WidgetData.today()))
                WidgetData.flash(c, key, if (a == "tmd") "Marked as done" else "Added to your goals")
            }
        }
        WidgetData.refreshAll(c)
    }

    private fun rowPi(c: Context, idx: Int): PendingIntent {
        val i = Intent(c, javaClass).setAction(ACT_ROW).putExtra("i", idx)
            .setData(Uri.parse("sustaina://row/$key/${javaClass.simpleName}/$idx"))
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun btnPi(c: Context, b: Btn, qi: Int): PendingIntent {
        if (b.act == "jr" || b.act == "rf") {
            val i = Intent(c, QuickEntryActivity::class.java).putExtra("mode", b.act).putExtra("qi", qi)
                .setData(Uri.parse("sustaina://entry/${b.act}"))
            return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val i = Intent(c, javaClass).setAction(ACT_BTN).putExtra("a", b.act)
            .setData(Uri.parse("sustaina://btn/$key/${javaClass.simpleName}/${b.act}"))
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun draw(c: Context, m: AppWidgetManager, id: Int) {
        val v = RemoteViews(c.packageName, R.layout.widget_generic)
        val t = Color.parseColor(textColor)
        val mu = Color.parseColor(mutedColor)
        v.setInt(R.id.root, "setBackgroundResource", bg)
        v.setTextViewText(R.id.wtitle, "SUSTAINA  \u00b7  $title")
        v.setTextColor(R.id.wtitle, mu)
        val rowIds = listOf(R.id.l1, R.id.l2, R.id.l3, R.id.l4)
        val btnIds = listOf(R.id.b1, R.id.b2, R.id.b3)
        rowIds.forEach { v.setViewVisibility(it, View.GONE) }
        btnIds.forEach { v.setViewVisibility(it, View.GONE) }
        v.setViewVisibility(R.id.brow, View.GONE)
        val d = WidgetData.get(c, key)
        val h = m.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        if (d == null) {
            v.setTextViewText(R.id.big, "Open SUSTAINA")
            v.setTextViewText(R.id.sub, "Open the app once to load your records.")
            v.setViewVisibility(R.id.bar, View.GONE)
        } else {
            val big = d.optString("big", "")
            val showBig = big.isNotEmpty() && h >= 150
            val p = d.optInt("p", -1)
            val showBar = p >= 0 && h >= 180
            val hasBtn = btns.isNotEmpty() && h >= 120
            val fixed = 28 + 16 + 20 + (if (showBig) 40 else 0) + (if (showBar) 16 else 0) + (if (hasBtn) 40 else 0)
            val slots = ((h - fixed) / 24).coerceIn(0, 4)
            v.setViewVisibility(R.id.big, if (showBig) View.VISIBLE else View.GONE)
            v.setTextViewText(R.id.big, big)
            val sub = d.optString("sub", "")
            v.setTextViewText(R.id.sub, if (!showBig && big.isNotEmpty()) "$big  \u00b7  $sub" else sub)
            if (showBar) { v.setViewVisibility(R.id.bar, View.VISIBLE); v.setProgressBar(R.id.bar, 100, p.coerceIn(0, 100), false) }
            else v.setViewVisibility(R.id.bar, View.GONE)
            val items = d.optJSONArray("items")
            val lines = d.optJSONArray("lines")
            for (i in 0 until slots) {
                if (items != null) {
                    val item = items.optJSONObject(i) ?: continue
                    val tappable = item.opt("k")?.toString().orEmpty().isNotEmpty()
                    v.setViewVisibility(rowIds[i], View.VISIBLE)
                    v.setTextViewText(rowIds[i], (if (item.optBoolean("on", false)) "\u2713  " else "\u25CB  ") + item.optString("t"))
                    v.setTextColor(rowIds[i], t)
                    if (tappable) v.setOnClickPendingIntent(rowIds[i], rowPi(c, i))
                } else if (lines != null && i < lines.length() && lines.optString(i).isNotEmpty()) {
                    v.setViewVisibility(rowIds[i], View.VISIBLE)
                    v.setTextViewText(rowIds[i], lines.optString(i))
                    v.setTextColor(rowIds[i], t)
                }
            }
            if (hasBtn) {
                v.setViewVisibility(R.id.brow, View.VISIBLE)
                btns.take(3).forEachIndexed { i, b ->
                    v.setViewVisibility(btnIds[i], View.VISIBLE)
                    v.setTextViewText(btnIds[i], b.label)
                    v.setTextColor(btnIds[i], t)
                    v.setOnClickPendingIntent(btnIds[i], btnPi(c, b, d.optInt("qi", 0)))
                }
            }
        }
        v.setTextColor(R.id.big, t); v.setTextColor(R.id.sub, mu)
        val open = Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, key.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        m.updateAppWidget(id, v)
    }
}
class WHabit : BaseWidget("habit", "Daily Habit Tracker", R.drawable.bg_habit, "#14532D", "#4B6B57", emptyList())
class WHabitM : BaseWidget("habit", "Daily Habit Tracker", R.drawable.bg_habit, "#14532D", "#4B6B57", emptyList())
class WHabitL : BaseWidget("habit", "Daily Habit Tracker", R.drawable.bg_habit, "#14532D", "#4B6B57", emptyList())
class WStreak : BaseWidget("streak", "Habit Streak Tracker", R.drawable.bg_streak, "#7C2D12", "#8A5A3C", emptyList())
class WStreakM : BaseWidget("streak", "Habit Streak Tracker", R.drawable.bg_streak, "#7C2D12", "#8A5A3C", emptyList())
class WStreakL : BaseWidget("streak", "Habit Streak Tracker", R.drawable.bg_streak, "#7C2D12", "#8A5A3C", emptyList())
class WJournal : BaseWidget("journal", "Daily Activity Journal", R.drawable.bg_journal, "#14532D", "#4B6B57", listOf(Btn("Add entry", "jr")))
class WJournalM : BaseWidget("journal", "Daily Activity Journal", R.drawable.bg_journal, "#14532D", "#4B6B57", listOf(Btn("Add entry", "jr")))
class WJournalL : BaseWidget("journal", "Daily Activity Journal", R.drawable.bg_journal, "#14532D", "#4B6B57", listOf(Btn("Add entry", "jr")))
class WWater : BaseWidget("water", "Water-Saving Tracker", R.drawable.bg_water, "#0C4A6E", "#3F6B82", emptyList())
class WWaterM : BaseWidget("water", "Water-Saving Tracker", R.drawable.bg_water, "#0C4A6E", "#3F6B82", emptyList())
class WWaterL : BaseWidget("water", "Water-Saving Tracker", R.drawable.bg_water, "#0C4A6E", "#3F6B82", emptyList())
class WWaste : BaseWidget("waste", "Waste Reduction Tracker", R.drawable.bg_waste, "#14532D", "#4B6B57", emptyList())
class WWasteM : BaseWidget("waste", "Waste Reduction Tracker", R.drawable.bg_waste, "#14532D", "#4B6B57", emptyList())
class WWasteL : BaseWidget("waste", "Waste Reduction Tracker", R.drawable.bg_waste, "#14532D", "#4B6B57", emptyList())
class WEnergy : BaseWidget("energy", "Energy-Saving Tracker", R.drawable.bg_energy, "#4D3A05", "#6B5A2A", emptyList())
class WEnergyM : BaseWidget("energy", "Energy-Saving Tracker", R.drawable.bg_energy, "#4D3A05", "#6B5A2A", emptyList())
class WEnergyL : BaseWidget("energy", "Energy-Saving Tracker", R.drawable.bg_energy, "#4D3A05", "#6B5A2A", emptyList())
class WReflect : BaseWidget("reflect", "Daily Reflection", R.drawable.bg_reflect, "#14532D", "#4B6B57", listOf(Btn("Write reflection", "rf")))
class WReflectM : BaseWidget("reflect", "Daily Reflection", R.drawable.bg_reflect, "#14532D", "#4B6B57", listOf(Btn("Write reflection", "rf")))
class WReflectL : BaseWidget("reflect", "Daily Reflection", R.drawable.bg_reflect, "#14532D", "#4B6B57", listOf(Btn("Write reflection", "rf")))
class WGoals : BaseWidget("goals", "Personal Goals Tracker", R.drawable.bg_goals, "#14532D", "#4B6B57", emptyList())
class WGoalsM : BaseWidget("goals", "Personal Goals Tracker", R.drawable.bg_goals, "#14532D", "#4B6B57", emptyList())
class WGoalsL : BaseWidget("goals", "Personal Goals Tracker", R.drawable.bg_goals, "#14532D", "#4B6B57", emptyList())
class WCal : BaseWidget("cal", "Habit Calendar", R.drawable.bg_cal, "#14532D", "#4B6B57", emptyList())
class WCalM : BaseWidget("cal", "Habit Calendar", R.drawable.bg_cal, "#14532D", "#4B6B57", emptyList())
class WCalL : BaseWidget("cal", "Habit Calendar", R.drawable.bg_cal, "#14532D", "#4B6B57", emptyList())
class WTmrw : BaseWidget("tmrw", "Tomorrow's Eco Action", R.drawable.bg_tmrw, "#14532D", "#4B6B57", listOf(Btn("Done", "tmd"), Btn("Goal", "tmg"), Btn("Another", "tmn")))
class WTmrwM : BaseWidget("tmrw", "Tomorrow's Eco Action", R.drawable.bg_tmrw, "#14532D", "#4B6B57", listOf(Btn("Done", "tmd"), Btn("Goal", "tmg"), Btn("Another", "tmn")))
class WTmrwL : BaseWidget("tmrw", "Tomorrow's Eco Action", R.drawable.bg_tmrw, "#14532D", "#4B6B57", listOf(Btn("Done", "tmd"), Btn("Goal", "tmg"), Btn("Another", "tmn")))
class WSum : BaseWidget("sum", "Daily Impact", R.drawable.bg_sum, "#14532D", "#4B6B57", emptyList())
class WSumM : BaseWidget("sum", "Daily Impact", R.drawable.bg_sum, "#14532D", "#4B6B57", emptyList())
class WSumL : BaseWidget("sum", "Daily Impact", R.drawable.bg_sum, "#14532D", "#4B6B57", emptyList())
