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
 * One widget = one key. The website works out numbers and text; this class draws them with the same
 * colours and art as the My Phone Widgets previews, and turns taps into queued actions (ActionStore).
 */
abstract class BaseWidget(
    private val key: String, val title: String,
    private val btns: List<Btn> = emptyList()
) : AppWidgetProvider() {
    companion object {
        const val ACT_ROW = "com.sustaina.app.ROW"
        const val ACT_BTN = "com.sustaina.app.BTN"
        // light text, light muted, light accent, dark text, dark muted, dark accent, accent code
        val CELLS = listOf(R.id.c0, R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5, R.id.c6, R.id.c7, R.id.c8, R.id.c9, R.id.c10, R.id.c11, R.id.c12, R.id.c13, R.id.c14, R.id.c15, R.id.c16, R.id.c17, R.id.c18, R.id.c19, R.id.c20, R.id.c21, R.id.c22, R.id.c23, R.id.c24, R.id.c25, R.id.c26, R.id.c27, R.id.c28, R.id.c29, R.id.c30, R.id.c31, R.id.c32, R.id.c33, R.id.c34, R.id.c35, R.id.c36, R.id.c37, R.id.c38, R.id.c39, R.id.c40, R.id.c41)
        val PAL = mapOf(
        "habit" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "streak" to arrayOf("#7c2d12", "#8a5a3c", "#ea580c", "#e9f8ef", "#a3c9b3", "#fb923c", "s"),
        "journal" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "water" to arrayOf("#0c4a6e", "#3f6b82", "#0e7490", "#e9f8ef", "#a3c9b3", "#38bdf8", "w"),
        "waste" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "energy" to arrayOf("#4d3a05", "#6b5a2a", "#ca8a04", "#e9f8ef", "#a3c9b3", "#facc15", "e"),
        "reflect" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "goals" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "cal" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "tmrw" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g"),
        "sum" to arrayOf("#14532d", "#4b6b57", "#16a34a", "#e9f8ef", "#a3c9b3", "#4ade80", "g")
        )
    }

    private fun rid(c: Context, name: String): Int = c.resources.getIdentifier(name, "drawable", c.packageName)

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

    override fun onDeleted(c: Context, ids: IntArray) { WidgetData.clearThemeFor(c, ids) }

    /** Used by the customise screen to redraw one widget after its theme was chosen. */
    fun redraw(c: Context, m: AppWidgetManager, id: Int) = draw(c, m, id)

    private fun draw(c: Context, m: AppWidgetManager, id: Int) {
        val o = m.getAppWidgetOptions(id)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        m.updateAppWidget(id, build(c, w, h, WidgetData.themeFor(c, id)))
    }

    /** Builds the widget picture for a size in dp. mode: "auto" (follow the app), "light" or "dark". */
    fun build(c: Context, w: Int, h: Int, mode: String): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_generic)
        val dark = when (mode) { "dark" -> true; "light" -> false; else -> WidgetData.theme(c) == "dark" }
        val sfx = if (dark) "_d" else ""
        val bs = WidgetData.bgStyle(c)
        val p = PAL[key] ?: PAL["habit"]!!
        val t = Color.parseColor(if (dark) p[3] else p[0])
        val mu = Color.parseColor(if (dark) p[4] else p[1])
        val acc = Color.parseColor(if (dark) p[5] else p[2])
        val ac = p[6]
        val dp = c.resources.displayMetrics.density
        v.setInt(R.id.root, "setBackgroundResource", rid(c, (if (bs == "solid") "bgs_" else "bg_") + key + sfx))
        val art = bs == "nature"
        v.setViewVisibility(R.id.hills, if (art) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.corner, if (art) View.VISIBLE else View.GONE)
        v.setInt(R.id.hills, "setColorFilter", acc)
        v.setInt(R.id.corner, "setColorFilter", acc)
        v.setInt(R.id.hleaf, "setColorFilter", acc)
        v.setTextViewText(R.id.wtitle, title)
        v.setTextColor(R.id.hbrand, t)
        v.setTextColor(R.id.wtitle, mu)
        val rowIds = listOf(R.id.l1, R.id.l2, R.id.l3, R.id.l4)
        val btnIds = listOf(R.id.b1, R.id.b2, R.id.b3)
        val wkIds = listOf(R.id.w1, R.id.w2, R.id.w3, R.id.w4, R.id.w5, R.id.w6, R.id.w7)
        rowIds.forEach { v.setViewVisibility(it, View.GONE) }
        btnIds.forEach { v.setViewVisibility(it, View.GONE) }
        v.setViewVisibility(R.id.brow, View.GONE)
        v.setViewVisibility(R.id.wk, View.GONE)
        v.setViewVisibility(R.id.grid, View.GONE)
        v.setViewVisibility(R.id.art, View.GONE)
        v.setViewPadding(R.id.content, (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
        val d = WidgetData.get(c, key)
        if (d == null) {
            v.setViewVisibility(R.id.big, View.GONE)
            v.setTextViewText(R.id.sub, "Open SUSTAINA once to load your records.")
            v.setViewVisibility(R.id.bar, View.GONE)
        } else {
            val wkArr = d.optJSONArray("wk")
            val grid = d.optJSONObject("grid")
            val showWk = wkArr != null && wkArr.length() == 7 && h >= 140
            val showGrid = grid != null && h >= 200
            val showPlant = key == "habit" && w >= 200 && h >= 120
            val big = d.optString("big", "")
            val showBig = big.isNotEmpty() && h >= 150 && !showGrid
            val p0 = d.optInt("p", -1)
            val showBar = p0 >= 0 && h >= 180 && !showWk && !showGrid
            val hasBtn = btns.isNotEmpty() && h >= 120
            val fixed = 28 + 16 + 20 + (if (showBig) 44 else 0) + (if (showBar) 16 else 0) + (if (hasBtn) 40 else 0) +
                (if (showWk) (if (key == "sum") 66 else 50) else 0) + (if (showGrid) 138 else 0)
            val slots = ((h - fixed) / 26).coerceIn(0, 4)
            v.setViewVisibility(R.id.big, if (showBig) View.VISIBLE else View.GONE)
            v.setTextViewText(R.id.big, big)
            val sub = d.optString("sub", "")
            v.setTextViewText(R.id.sub, if (!showBig && big.isNotEmpty() && !showGrid) "$big  \u00b7  $sub" else sub)
            if (showBar) { v.setViewVisibility(R.id.bar, View.VISIBLE); v.setProgressBar(R.id.bar, 100, p0.coerceIn(0, 100), false) }
            else v.setViewVisibility(R.id.bar, View.GONE)
            if (showPlant) {
                val stage = if (p0 >= 100) 4 else if (p0 >= 70) 3 else if (p0 >= 35) 2 else if (p0 > 0) 1 else 0
                v.setViewVisibility(R.id.art, View.VISIBLE)
                v.setImageViewResource(R.id.art, rid(c, "plant_$stage$sfx"))
                v.setViewPadding(R.id.content, (14 * dp).toInt(), (14 * dp).toInt(), (74 * dp).toInt(), (14 * dp).toInt())
            }
            if (showWk && wkArr != null) {
                v.setViewVisibility(R.id.wk, View.VISIBLE)
                val labs = "MTWTFSS"
                val bars = d.optJSONArray("bars")
                for (i in 0 until 7) {
                    v.setTextViewText(wkIds[i], labs[i].toString())
                    v.setTextColor(wkIds[i], mu)
                    val dr = if (key == "sum" && bars != null) rid(c, "bar_" + bars.optInt(i, 0).coerceIn(0, 10) + sfx)
                             else rid(c, (if (wkArr.optInt(i, 0) == 1) "ck_on_" else "ck_off_") + ac + sfx)
                    v.setTextViewCompoundDrawables(wkIds[i], 0, dr, 0, 0)
                }
            }
            if (showGrid && grid != null) {
                v.setViewVisibility(R.id.grid, View.VISIBLE)
                val rowGrp = listOf(R.id.r0, R.id.r1, R.id.r2, R.id.r3, R.id.r4, R.id.r5)
                val lead = grid.optInt("lead", 0)
                val days = grid.optInt("days", 30)
                val today = grid.optInt("today", 0)
                val rec = HashSet<Int>()
                val ra = grid.optJSONArray("rec")
                if (ra != null) for (i in 0 until ra.length()) rec.add(ra.optInt(i))
                for (r in 0 until 6) {
                    var any = false
                    for (cc in 0 until 7) {
                        val cid = CELLS[r * 7 + cc]
                        val day = r * 7 + cc - lead + 1
                        if (day in 1..days) {
                            any = true
                            v.setViewVisibility(cid, View.VISIBLE)
                            v.setTextViewText(cid, day.toString())
                            if (rec.contains(day)) {
                                v.setInt(cid, "setBackgroundResource", rid(c, "dot_g$sfx"))
                                v.setTextColor(cid, Color.parseColor(if (dark) "#052e16" else "#ffffff"))
                            } else {
                                v.setInt(cid, "setBackgroundResource", if (day == today) rid(c, "ring_g$sfx") else 0)
                                v.setTextColor(cid, t)
                            }
                        } else {
                            v.setViewVisibility(cid, View.INVISIBLE)
                        }
                    }
                    v.setViewVisibility(rowGrp[r], if (any) View.VISIBLE else View.GONE)
                }
            }
            val items = d.optJSONArray("items")
            val lines = d.optJSONArray("lines")
            for (i in 0 until slots) {
                if (items != null) {
                    val item = items.optJSONObject(i) ?: continue
                    val tappable = item.opt("k")?.toString().orEmpty().isNotEmpty()
                    v.setViewVisibility(rowIds[i], View.VISIBLE)
                    v.setTextViewText(rowIds[i], item.optString("t"))
                    v.setTextColor(rowIds[i], t)
                    val ck = rid(c, (if (item.optBoolean("on", false)) "ck_on_" else "ck_off_") + ac + sfx)
                    v.setTextViewCompoundDrawablesRelative(rowIds[i], ck, 0, 0, 0)
                    if (tappable) v.setOnClickPendingIntent(rowIds[i], rowPi(c, i))
                } else if (lines != null && i < lines.length() && lines.optString(i).isNotEmpty()) {
                    v.setViewVisibility(rowIds[i], View.VISIBLE)
                    v.setTextViewText(rowIds[i], lines.optString(i))
                    v.setTextColor(rowIds[i], t)
                    v.setTextViewCompoundDrawablesRelative(rowIds[i], 0, 0, 0, 0)
                }
            }
            if (hasBtn) {
                v.setViewVisibility(R.id.brow, View.VISIBLE)
                btns.take(3).forEachIndexed { i, b ->
                    v.setViewVisibility(btnIds[i], View.VISIBLE)
                    v.setTextViewText(btnIds[i], b.label)
                    v.setInt(btnIds[i], "setBackgroundResource", rid(c, if (i == 0) "pill_$ac$sfx" else "soft$sfx"))
                    v.setTextColor(btnIds[i], if (i == 0) Color.parseColor(if (dark) "#052e16" else "#ffffff") else t)
                    v.setOnClickPendingIntent(btnIds[i], btnPi(c, b, d.optInt("qi", 0)))
                }
            }
        }
        v.setTextColor(R.id.big, t); v.setTextColor(R.id.sub, mu)
        val open = Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, key.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        return v
    }
}

class WHabit : BaseWidget("habit", "Daily Habit Tracker", emptyList())
class WHabitM : BaseWidget("habit", "Daily Habit Tracker", emptyList())
class WHabitL : BaseWidget("habit", "Daily Habit Tracker", emptyList())
class WStreak : BaseWidget("streak", "Habit Streak Tracker", emptyList())
class WStreakM : BaseWidget("streak", "Habit Streak Tracker", emptyList())
class WStreakL : BaseWidget("streak", "Habit Streak Tracker", emptyList())
class WJournal : BaseWidget("journal", "Daily Activity Journal", listOf(Btn("Add entry", "jr")))
class WJournalM : BaseWidget("journal", "Daily Activity Journal", listOf(Btn("Add entry", "jr")))
class WJournalL : BaseWidget("journal", "Daily Activity Journal", listOf(Btn("Add entry", "jr")))
class WWater : BaseWidget("water", "Water-Saving Tracker", emptyList())
class WWaterM : BaseWidget("water", "Water-Saving Tracker", emptyList())
class WWaterL : BaseWidget("water", "Water-Saving Tracker", emptyList())
class WWaste : BaseWidget("waste", "Waste Reduction Tracker", emptyList())
class WWasteM : BaseWidget("waste", "Waste Reduction Tracker", emptyList())
class WWasteL : BaseWidget("waste", "Waste Reduction Tracker", emptyList())
class WEnergy : BaseWidget("energy", "Energy-Saving Tracker", emptyList())
class WEnergyM : BaseWidget("energy", "Energy-Saving Tracker", emptyList())
class WEnergyL : BaseWidget("energy", "Energy-Saving Tracker", emptyList())
class WReflect : BaseWidget("reflect", "Daily Reflection", listOf(Btn("Write reflection", "rf")))
class WReflectM : BaseWidget("reflect", "Daily Reflection", listOf(Btn("Write reflection", "rf")))
class WReflectL : BaseWidget("reflect", "Daily Reflection", listOf(Btn("Write reflection", "rf")))
class WGoals : BaseWidget("goals", "Personal Goals Tracker", emptyList())
class WGoalsM : BaseWidget("goals", "Personal Goals Tracker", emptyList())
class WGoalsL : BaseWidget("goals", "Personal Goals Tracker", emptyList())
class WCal : BaseWidget("cal", "Habit Calendar", emptyList())
class WCalM : BaseWidget("cal", "Habit Calendar", emptyList())
class WCalL : BaseWidget("cal", "Habit Calendar", emptyList())
class WTmrw : BaseWidget("tmrw", "Tomorrow's Eco Action", listOf(Btn("Done", "tmd"), Btn("Goal", "tmg"), Btn("Another", "tmn")))
class WTmrwM : BaseWidget("tmrw", "Tomorrow's Eco Action", listOf(Btn("Done", "tmd"), Btn("Goal", "tmg"), Btn("Another", "tmn")))
class WTmrwL : BaseWidget("tmrw", "Tomorrow's Eco Action", listOf(Btn("Done", "tmd"), Btn("Goal", "tmg"), Btn("Another", "tmn")))
class WSum : BaseWidget("sum", "Daily Impact", emptyList())
class WSumM : BaseWidget("sum", "Daily Impact", emptyList())
class WSumL : BaseWidget("sum", "Daily Impact", emptyList())
