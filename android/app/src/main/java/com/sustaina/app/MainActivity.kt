package com.sustaina.app

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import org.json.JSONArray
import com.sustaina.app.widgets.ActionStore
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sustaina.app.widgets.WidgetData

const val SITE = "https://sustaina-sdg11.vercel.app/"

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var chooser: ValueCallback<Array<Uri>>? = null

    /** The website calls this to hand the widgets a fresh copy of the user's numbers. */
    inner class Bridge {
        @JavascriptInterface
        fun setWidgetData(json: String) {
            // Do not overwrite the widgets while home-screen actions are still waiting to be applied.
            if (ActionStore.all(applicationContext).length() > 0) return
            WidgetData.save(applicationContext, json)
            WidgetData.refreshAll(applicationContext)
        }

        /** Actions done on the home screen that the website has not applied yet. */
        @JavascriptInterface
        fun pendingActions(): String = ActionStore.all(applicationContext).toString()

        @JavascriptInterface
        fun ackActions(ids: String) {
            try {
                val a = JSONArray(ids)
                val s = HashSet<String>()
                for (i in 0 until a.length()) s.add(a.optString(i))
                ActionStore.ack(applicationContext, s)
                WidgetData.refreshAll(applicationContext)
            } catch (e: Exception) { }
        }

        @JavascriptInterface
        fun pinWidget(key: String, size: String) {
            runOnUiThread {
                val cls = WidgetData.classFor(key, size)
                val m = AppWidgetManager.getInstance(this@MainActivity)
                if (cls == null) {
                    Toast.makeText(this@MainActivity, "Could not find this widget", Toast.LENGTH_SHORT).show()
                } else if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported) {
                    m.requestPinAppWidget(ComponentName(this@MainActivity, cls), null, null)
                    Toast.makeText(this@MainActivity, "Confirm on the Android prompt to add it", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Your phone cannot add it from here. Press and hold the home screen, tap Widgets, then drag SUSTAINA.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B1220")
        web = WebView(this)
        setContentView(web)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mediaPlaybackRequiresUserGesture = false
        web.addJavascriptInterface(Bridge(), "SustainaAndroid")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                if (r == null) return false
                val h = r.url.host ?: return false
                val inside = h.endsWith("vercel.app") || h.endsWith("firebaseapp.com") ||
                    h.endsWith("googleapis.com") || h.endsWith("gstatic.com") || h.endsWith("google.com")
                if (inside) return false
                startActivity(Intent(Intent.ACTION_VIEW, r.url))
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView?, cb: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                if (cb == null || p == null) return false
                chooser?.onReceiveValue(null)
                chooser = cb
                return try { startActivityForResult(p.createIntent(), 77); true } catch (e: Exception) { chooser = null; false }
            }
        }
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(SITE)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 77) {
            chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data))
            chooser = null
        } else super.onActivityResult(req, res, data)
    }

    override fun onSaveInstanceState(b: Bundle) { super.onSaveInstanceState(b); web.saveState(b) }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
    override fun onResume() { super.onResume(); web.evaluateJavascript("try{wgApply();pwPush()}catch(e){}", null) }
    override fun onPause() { super.onPause(); web.evaluateJavascript("try{pwPush()}catch(e){}", null) }
}
