package com.sustaina.app.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores the last numbers the website sent. Stays on the phone only. */
object WidgetData {
    private const val PREF = "sustaina_widgets"
    private val BASE = mapOf(
        "habit" to "WHabit", "streak" to "WStreak", "journal" to "WJournal", "water" to "WWater",
        "waste" to "WWaste", "energy" to "WEnergy", "reflect" to "WReflect", "goals" to "WGoals",
        "cal" to "WCal", "tmrw" to "WTmrw", "sum" to "WSum"
    )

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun load(c: Context): JSONObject? {
        val raw = c.getSharedPreferences(PREF, 0).getString("d", null) ?: return null
        return try { JSONObject(raw) } catch (e: Exception) { null }
    }

    fun theme(c: Context): String = load(c)?.optString("st", "light") ?: "light"

    fun bgStyle(c: Context): String = load(c)?.optString("bs", "nature") ?: "nature"

    // Light/Dark choice for each widget on the home screen: "auto" (follow the app), "light" or "dark".
    private const val CFG = "sustaina_widget_cfg"
    fun themeFor(c: Context, id: Int): String = c.getSharedPreferences(CFG, 0).getString("t$id", "auto") ?: "auto"
    fun setThemeFor(c: Context, id: Int, mode: String) { c.getSharedPreferences(CFG, 0).edit().putString("t$id", mode).apply() }
    fun clearThemeFor(c: Context, ids: IntArray) {
        val e = c.getSharedPreferences(CFG, 0).edit()
        for (i in ids) e.remove("t$i")
        e.apply()
    }

    fun save(c: Context, json: String) { c.getSharedPreferences(PREF, 0).edit().putString("d", json).apply() }

    fun get(c: Context, key: String): JSONObject? = load(c)?.optJSONObject("w")?.optJSONObject(key)

    fun update(c: Context, key: String, f: (JSONObject) -> Unit) {
        val root = load(c) ?: return
        val o = root.optJSONObject("w")?.optJSONObject(key) ?: return
        f(o)
        c.getSharedPreferences(PREF, 0).edit().putString("d", root.toString()).apply()
    }

    /** Shows the tick straight away. The website applies the same action to the real records later. */
    fun setItem(c: Context, key: String, idx: Int, on: Boolean) {
        update(c, key) { o ->
            o.optJSONArray("items")?.optJSONObject(idx)?.put("on", on)
            if (key == "habit") {
                val dg = o.optInt("dg", 5).coerceAtLeast(1)
                val n = (o.optInt("n", 0) + (if (on) 1 else -1)).coerceAtLeast(0)
                o.put("n", n)
                o.put("big", "$n/$dg")
                o.put("p", (n * 100 / dg).coerceAtMost(100))
            }
        }
    }

    fun flash(c: Context, key: String, msg: String) {
        update(c, key) { o ->
            val l = JSONArray()
            l.put(o.optJSONArray("lines")?.optString(0) ?: "")
            l.put(msg)
            o.put("lines", l)
        }
    }

    fun nextIdea(c: Context, key: String) {
        var k = ""
        update(c, key) { o ->
            val alts = o.optJSONArray("alts")
            if (alts != null && alts.length() > 0) {
                val i = (o.optInt("ai", 0) + 1) % alts.length()
                val a = alts.getJSONObject(i)
                k = a.opt("k")?.toString() ?: ""
                o.put("ai", i)
                o.put("k", k)
                o.put("lines", JSONArray().put(a.optString("t")))
            }
        }
        if (k.isNotEmpty()) ActionStore.add(c, JSONObject().put("t", "tmp").put("k", k).put("d", today()))
    }

    fun afterEntry(c: Context, mode: String, text: String) {
        if (mode == "jr") {
            update(c, "journal") { o ->
                val old = o.optJSONArray("lines")
                val n = JSONArray()
                n.put(text)
                if (old != null) { for (i in 0 until minOf(old.length(), 3)) n.put(old.optString(i)) }
                o.put("lines", n)
                o.put("big", ((o.optString("big", "0").toIntOrNull() ?: 0) + 1).toString())
            }
        } else {
            update(c, "reflect") { o ->
                val l = JSONArray()
                l.put(o.optJSONArray("lines")?.optString(0) ?: "")
                l.put("Written today")
                o.put("lines", l)
            }
        }
    }

    fun classFor(key: String, size: String): Class<*>? {
        val b = BASE[key] ?: return null
        val s = when (size.lowercase()) { "m", "medium" -> "M"; "l", "large" -> "L"; else -> "" }
        return try { Class.forName("com.sustaina.app.widgets.$b$s") } catch (e: Exception) { null }
    }

    val ALL: List<Class<*>> by lazy {
        val out = ArrayList<Class<*>>()
        for (b in BASE.values) for (s in listOf("", "M", "L")) {
            try { out.add(Class.forName("com.sustaina.app.widgets.$b$s")) } catch (e: Exception) { }
        }
        out
    }

    fun refreshAll(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        for (cls in ALL) {
            val ids = m.getAppWidgetIds(ComponentName(c, cls))
            if (ids.isNotEmpty()) c.sendBroadcast(Intent(c, cls).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}
