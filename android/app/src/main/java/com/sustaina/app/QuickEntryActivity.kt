package com.sustaina.app

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sustaina.app.widgets.ActionStore
import com.sustaina.app.widgets.WidgetData
import org.json.JSONObject

/** Small pop-up opened from a widget so the user can type a note without opening SUSTAINA. */
class QuickEntryActivity : Activity() {
    private lateinit var main: EditText
    private var note: EditText? = null
    private var mode = "jr"

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        mode = intent.getStringExtra("mode") ?: "jr"
        val qi = intent.getIntExtra("qi", 0).coerceIn(0, 2)
        val qs = arrayOf("What was your best eco-friendly action today?", "What habit was difficult to maintain?", "What would you like to improve tomorrow?")
        val pad = (16 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad, pad, pad)
        val head = TextView(this)
        head.text = if (mode == "rf") qs[qi] else "What did you do for the planet?"
        head.textSize = 16f
        box.addView(head)
        val prefs = getSharedPreferences("sustaina_draft", 0)
        main = EditText(this)
        main.hint = if (mode == "rf") "Write your reflection" else "Short description"
        main.minLines = if (mode == "rf") 4 else 2
        main.setText(prefs.getString("t_$mode", ""))
        box.addView(main, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (mode == "jr") {
            val n = EditText(this)
            n.hint = "Note (optional)"
            n.setText(prefs.getString("n_$mode", ""))
            box.addView(n, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            note = n
        }
        val save = Button(this)
        save.text = "Save"
        save.setOnClickListener {
            val text = main.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Write something first", Toast.LENGTH_SHORT).show()
            } else {
                val o = JSONObject().put("t", mode).put("d", WidgetData.today()).put("x", text)
                if (mode == "jr") o.put("n", note?.text?.toString()?.trim() ?: "") else o.put("qi", qi)
                ActionStore.add(this, o)
                prefs.edit().remove("t_$mode").remove("n_$mode").apply()
                WidgetData.afterEntry(this, mode, text)
                WidgetData.refreshAll(this)
                Toast.makeText(this, "Saved. It shows in SUSTAINA the next time you open the app.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        box.addView(save)
        setContentView(box)
    }

    /** Keeps what was typed if Android closes this pop-up before Save. */
    override fun onPause() {
        super.onPause()
        if (isFinishing) return
        getSharedPreferences("sustaina_draft", 0).edit()
            .putString("t_$mode", main.text.toString()).putString("n_$mode", note?.text?.toString() ?: "").apply()
    }
}
