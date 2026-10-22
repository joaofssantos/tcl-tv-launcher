package com.joaofsantos.tcllauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private data class AppArtwork(val drawable: Drawable?, val isBanner: Boolean)
    private lateinit var config: JSONObject
    private var rail: HorizontalScrollView? = null
    private val dp get() = resources.displayMetrics.density
    private fun px(value: Int) = (value * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = JSONObject(assets.open("launcher_config.json").bufferedReader().use { it.readText() })
        inputSwitcher = TvInputSwitcher(this)
        setContentView(buildHome())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        rail?.scrollTo(0, 0)
        findViewById<View>(FEATURED_ID)?.requestFocus()
    }

    override fun onResume() { super.onResume(); window.decorView.postDelayed({ findViewById<View>(FEATURED_ID)?.requestFocus() }, 180) }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.post { findViewById<View>(FEATURED_ID)?.requestFocus() }
    }

    private fun buildHome(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false; setBackgroundColor(Color.rgb(18,22,31)); setPadding(px(48), px(28), px(48), px(24)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; clipChildren = false }
        header.addView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(label("Para você", 40f).apply { typeface = Typeface.DEFAULT_BOLD }); addView(label("${greeting()} · ${dateLabel()}", 13f).apply { setTextColor(Color.rgb(154,164,181)) }) }, LinearLayout.LayoutParams(0, px(70), 1f))
        header.addView(roundAction("", R.drawable.ic_input) { inputSwitcher?.switchTo(config.getString("rokuInputId")) }, LinearLayout.LayoutParams(px(44), px(44)).apply { marginEnd = px(10) })
        header.addView(roundAction("", R.drawable.ic_wifi) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }, LinearLayout.LayoutParams(px(44), px(44)).apply { marginEnd = px(10) })
        header.addView(roundAction("", R.drawable.ic_settings) { startActivity(Intent(Settings.ACTION_SETTINGS)) }.apply { id = View.generateViewId(); nextFocusRightId = id }, LinearLayout.LayoutParams(px(44), px(44)).apply { marginEnd = px(18) })
        header.addView(label(clock(), 22f))
        root.addView(header)
        val primary = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; clipChildren = false }
        val youtube = LauncherCard(this, "").apply { id = FEATURED_ID; setBrandColor(Color.rgb(30,37,51)); setArtwork(getDrawable(R.drawable.ic_youtube_play), true, true); setOnClickListener { openYouTubeSubscriptions("com.google.android.youtube.tv") } }
        primary.addView(primaryTile(youtube, "YouTube", "Inscrições"), LinearLayout.LayoutParams(0, px(286), 1f).apply { marginEnd = px(16) })
        val prime = LauncherCard(this, "").apply { setBrandGradient(Color.rgb(0,168,225), Color.rgb(15,121,175)); setArtwork(appArtwork("com.amazon.amazonvideo.livingroom").drawable, true, true); setOnClickListener { openPackage("com.amazon.amazonvideo.livingroom") } }
        primary.addView(primaryTile(prime, "Prime Video", "Séries e filmes"), LinearLayout.LayoutParams(0, px(286), 1f).apply { marginEnd = px(16) })
        val roku = LauncherCard(this, "").apply { setBrandColor(Color.rgb(108,60,151)); id = View.generateViewId(); nextFocusRightId = id; setArtwork(getDrawable(R.drawable.ic_hdmi_input), true, true); setOnClickListener { inputSwitcher?.switchTo(config.getString("rokuInputId")) } }
        primary.addView(primaryTile(roku, "Roku", "Entrada HDMI 1"), LinearLayout.LayoutParams(0, px(286), 1f))
        root.addView(primary, LinearLayout.LayoutParams(-1, 0, 1f))
        val rail = HorizontalScrollView(this).also { rail = it }.apply { isHorizontalScrollBarEnabled = false; background = GradientDrawable().apply { cornerRadius = px(28).toFloat(); setColor(Color.argb(15,255,255,255)) }; setPadding(px(14),px(14),px(14),px(14)) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
        val excluded = setOf(packageName, "com.google.android.youtube.tv", "com.amazon.amazonvideo.livingroom", "com.android.tv.settings", "com.tcl.tv", "com.tcl.dashboard", "com.tcl.partnercustomizer", "com.tcl.suspension")
        val activities = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName !in excluded }.sortedWith(compareBy<ResolveInfo> { when(it.activityInfo.packageName) { "com.netflix.ninja" -> 0; "com.globo.globotv" -> 1; "com.google.android.youtube.tvmusic" -> 2; else -> 3 } }.thenBy { it.loadLabel(packageManager).toString() })
        val cards = mutableListOf<LauncherCard>()
        activities.forEach { info ->
            val pkg = info.activityInfo.packageName
            val card = LauncherCard(this, info.loadLabel(packageManager).toString()).apply { id = View.generateViewId(); setArtwork(appArtwork(pkg).drawable); setOnClickListener { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setClassName(info.activityInfo.packageName, info.activityInfo.name)) } }
            cards += card
            row.addView(card, LinearLayout.LayoutParams(px(132), px(80)).apply { marginEnd = px(12) })
        }
        // Each grid tile goes down to the carousel card under it.
        listOf(youtube to 0, prime to 2, roku to 4).forEach { (tile, index) -> tile.nextFocusDownId = cards.getOrNull(minOf(index, cards.size - 1))?.id ?: View.NO_ID }
        rail.addView(row)
        root.addView(rail, LinearLayout.LayoutParams(-1, px(120)))
        return root
    }
    private fun primaryTile(card: LauncherCard, title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        addView(card, LinearLayout.LayoutParams(-1, px(236)))
        addView(label(title, 16f).apply { typeface = Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(-1, px(26)).apply { topMargin = px(8) })
        addView(label(subtitle, 13f).apply { setTextColor(Color.rgb(154,164,181)) }, LinearLayout.LayoutParams(-1, px(18)))
    }
    private fun roundAction(symbol: String, icon: Int? = null, action: () -> Unit) = TextView(this).apply {
        text = symbol; textSize = 23f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        if (icon != null) { setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0); setPadding(0, px(10), 0, 0) }
        isFocusable = true; isClickable = true; background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(20, 255, 255, 255)) }
        setOnClickListener { action() }
        onFocusChangeListener = View.OnFocusChangeListener { view, focused ->
            (view.background as GradientDrawable).apply { setColor(Color.argb(if (focused) 60 else 20, 255, 255, 255)); setStroke(if (focused) px(3) else 0, Color.WHITE) }
            view.animate().scaleX(if (focused) 1.1f else 1f).scaleY(if (focused) 1.1f else 1f).setDuration(120).start()
        }
    }
    private fun dockParams() = LinearLayout.LayoutParams(0, -1, 1f).apply { marginEnd = px(12) }
    private fun label(text: String, size: Float) = TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL }
    private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    private fun clock() = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date())
    private fun dateLabel() = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
    private fun appArtwork(packageName: String): AppArtwork {
        val activity = packageManager.resolveActivity(leanbackIntent(packageName), PackageManager.MATCH_ALL)
        val banner = activity?.activityInfo?.loadBanner(packageManager)
            ?: runCatching { packageManager.getApplicationBanner(packageName) }.getOrNull()
        return if (banner != null) AppArtwork(banner, true) else AppArtwork(activity?.loadIcon(packageManager)
            ?: runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull(), false)
    }
    private fun leanbackIntent(packageName: String) = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(packageName)
    private fun openPackage(packageName: String) {
        val activity = packageManager.resolveActivity(leanbackIntent(packageName), PackageManager.MATCH_ALL)
        val intent = activity?.activityInfo?.let { leanbackIntent(packageName).setClassName(it.packageName, it.name) }
            ?: packageManager.getLaunchIntentForPackage(packageName)
        intent?.let(::startActivity) ?: Toast.makeText(this, "App não instalado", Toast.LENGTH_SHORT).show()
    }
    private fun openYouTubeSubscriptions(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/subscriptions")).setPackage(packageName))
        } catch (_: Exception) {
            openPackage(packageName)
        }
    }
    private fun showAllApps() {
        val activities = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), PackageManager.MATCH_ALL).sortedBy { it.loadLabel(packageManager).toString() }
        val names = activities.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Todos os apps").setItems(names) { _, i ->
            activities[i].activityInfo.let { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setClassName(it.packageName, it.name)) }
        }.show()
    }
    /** Assigned by Session B when its TV input controller is available. */
    var inputSwitcher: InputSwitcher? = null
    companion object { private const val FEATURED_ID = 4001 }
}
