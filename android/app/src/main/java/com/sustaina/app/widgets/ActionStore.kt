package com.sustaina.app.widgets

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Actions done on the home screen wait here until the website applies them to the real records. */
object ActionStore {
    private const val PREF = "sustaina_outbox"

    @Synchronized
    fun add(c: Context, o: JSONObject) {
        val a = all(c)
        o.put("id", UUID.randomUUID().toString())
        a.put(o)
        c.getSharedPreferences(PREF, 0).edit().putString("a", a.toString()).apply()
    }

    fun all(c: Context): JSONArray =
        try { JSONArray(c.getSharedPreferences(PREF, 0).getString("a", "[]")) } catch (e: Exception) { JSONArray() }

    @Synchronized
    fun ack(c: Context, ids: Set<String>) {
        val a = all(c)
        val n = JSONArray()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (!ids.contains(o.optString("id"))) n.put(o)
        }
        c.getSharedPreferences(PREF, 0).edit().putString("a", n.toString()).apply()
    }
}
