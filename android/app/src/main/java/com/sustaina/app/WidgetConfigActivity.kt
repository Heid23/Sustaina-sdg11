package com.sustaina.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import com.sustaina.app.widgets.BaseWidget
import com.sustaina.app.widgets.WidgetData

/**
 * Opens when a widget is placed on the home screen (and when it is reconfigured on newer Android).
 * The person picks Light, Dark or "match the app" for this one widget and sees it before adding it.
 */
class WidgetConfigActivity : Activity() {
    private var id = AppWidgetManager.INVALID_APPWIDGET_ID
    private var provider: BaseWidget? = null
    private var mode = "auto"
    private var wDp = 110
    private var hDp = 110
    private var holder: FrameLayout? = null

    /** Shows the widget but swallows touches, so tapping the preview never records anything. */
    private class Shield(c: Context) : FrameLayout(c) {
        override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setResult(RESULT_CANCELED)
        id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val m = AppWidgetManager.getInstance(this)
        val info = m.getAppWidgetInfo(id)
        if (info == null) { finish(); return }
        provider = try {
            Class.forName(info.provider.className).getDeclaredConstructor().newInstance() as BaseWidget
        } catch (e: Exception) { null }
        val p = provider
        if (p == null) { finish(); return }
        val dm = resources.displayMetrics
        val dp = dm.density
        wDp = (info.minWidth / dp).toInt().coerceAtLeast(110)
        hDp = (info.minHeight / dp).toInt().coerceAtLeast(110)
        mode = WidgetData.themeFor(this, id)

        fun px(v: Int) = (v * dp).toInt()
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(px(20), px(24), px(20), px(24))

        val head = TextView(this)
        head.text = "Customize widget"
        head.textSize = 22f
        head.setTextColor(Color.parseColor("#0f172a"))
        head.setTypeface(head.typeface, android.graphics.Typeface.BOLD)
        col.addView(head)

        val name = TextView(this)
        name.text = p.title
        name.textSize = 14f
        name.setTextColor(Color.parseColor("#475569"))
        name.setPadding(0, px(2), 0, px(14))
        col.addView(name)

        val shield = Shield(this)
        val maxW = dm.widthPixels - px(40)
        val wPx = minOf(px(wDp), maxW)
        val hPx = if (px(wDp) > maxW) (px(hDp) * (wPx.toFloat() / px(wDp))).toInt() else px(hDp)
        col.addView(shield, LinearLayout.LayoutParams(wPx, hPx))
        holder = shield

        val lab = TextView(this)
        lab.text = "Appearance"
        lab.textSize = 14f
        lab.setTextColor(Color.parseColor("#0f172a"))
        lab.setTypeface(lab.typeface, android.graphics.Typeface.BOLD)
        lab.setPadding(0, px(18), 0, px(4))
        col.addView(lab)

        val group = RadioGroup(this)
        val opts = listOf("auto" to "Match the app setting", "light" to "Light", "dark" to "Dark")
        val ids = HashMap<String, Int>()
        for ((k, t) in opts) {
            val r = RadioButton(this)
            r.id = View.generateViewId()
            r.text = t
            r.textSize = 16f
            r.setPadding(px(6), px(8), 0, px(8))
            group.addView(r, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            ids[k] = r.id
        }
        group.check(ids[mode] ?: ids["auto"]!!)
        group.setOnCheckedChangeListener { _, checked ->
            for ((k, v) in ids) if (v == checked) mode = k
            render()
        }
        col.addView(group)

        val hint = TextView(this)
        hint.text = "Tap the checklist rows and buttons on the widget itself to record things. Changes appear in SUSTAINA the next time you open it."
        hint.textSize = 12f
        hint.setTextColor(Color.parseColor("#64748b"))
        hint.setPadding(0, px(10), 0, px(14))
        col.addView(hint)

        val add = Button(this)
        add.text = "Add widget"
        add.setOnClickListener { finishWith(p, m) }
        col.addView(add, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.parseColor("#f1f5f9"))
        sv.addView(col)
        setContentView(sv)
        render()
    }

    private fun render() {
        val h = holder ?: return
        val p = provider ?: return
        try {
            h.removeAllViews()
            val rv = p.build(applicationContext, wDp, hDp, mode)
            val v = rv.apply(applicationContext, h)
            h.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } catch (e: Exception) { }
    }

    private fun finishWith(p: BaseWidget, m: AppWidgetManager) {
        WidgetData.setThemeFor(this, id, mode)
        try { p.redraw(applicationContext, m, id) } catch (e: Exception) { }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}
