package com.joaofsantos.tcllauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private data class AppArtwork(val drawable: Drawable?, val isBanner: Boolean)
    private lateinit var config: JSONObject
    private val dp get() = resources.displayMetrics.density
    private fun px(value: Int) = (value * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = JSONObject(assets.open("launcher_config.json").bufferedReader().use { it.readText() })
        inputSwitcher = TvInputSwitcher(this)
        setContentView(buildHome())
    }

    override fun onResume() { super.onResume(); window.decorView.postDelayed({ findViewById<View>(FEATURED_ID)?.requestFocus() }, 180) }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.decorView.post { findViewById<View>(FEATURED_ID)?.requestFocus() }
    }

    private fun buildHome(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false; background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(11,15,23), Color.rgb(22,28,42))); setPadding(px(48), px(28), px(48), px(24)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; addView(label(greeting(), 27f)); addView(label(dateLabel(), 13f).apply { setTextColor(Color.LTGRAY) }) }, LinearLayout.LayoutParams(0, px(58), 1f))
        header.addView(label(clock(), 24f))
        root.addView(header)

        val nowPlaying = NowPlayingCard(this)
        root.addView(nowPlaying, LinearLayout.LayoutParams(-1, px(76)))
        val content = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; clipChildren = false }
        val featuredData = config.getJSONObject("featured")
        val featured = LauncherCard(this, featuredData.getString("label"), "Inscrições e vídeos novos").apply {
            id = FEATURED_ID
            setBrandGradient(Color.rgb(196, 48, 43), Color.rgb(122, 21, 18))
            setArtwork(getDrawable(R.drawable.ic_youtube_play), large = true)
            setOnClickListener { openYouTubeSubscriptions(featuredData.getString("package")) }
        }
        content.addView(featured, LinearLayout.LayoutParams(0, px(275), 1.25f).apply { marginEnd = px(28) })
        val continuing = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        continuing.addView(label("Continuar série", 21f), LinearLayout.LayoutParams(-1, px(34)))
        val series = config.getJSONArray("series")
        repeat(series.length()) { index ->
            val item = series.getJSONObject(index)
            val accents = intArrayOf(Color.rgb(201,162,39), Color.rgb(59,130,246), Color.rgb(185,28,28))
            val card = LauncherCard(this, item.getString("title"), item.getString("subtitle")).apply { setBrandColor(accents[index]); setOnClickListener { SeriesLauncher(this@MainActivity).open(item.getString("deepLink")) } }
            continuing.addView(card, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = px(7) })
        }
        content.addView(continuing, LinearLayout.LayoutParams(0, px(275), 1f))
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(label("Apps", 18f), LinearLayout.LayoutParams(-1, px(31)))
        val dock = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = GradientDrawable().apply { cornerRadius = px(18).toFloat(); setColor(Color.argb(22, 255, 255, 255)) }; setPadding(px(6), px(6), px(6), px(6)) }
        val apps = config.getJSONArray("dock")
        var firstDock: LauncherCard? = null
        repeat(apps.length()) { index ->
            val app = apps.getJSONObject(index)
            val card = LauncherCard(this, app.getString("label")).apply {
                id = View.generateViewId()
                nextFocusUpId = FEATURED_ID
                appArtwork(app.getString("package")).let { setArtwork(it.drawable, hideLabel = it.isBanner) }
                setOnClickListener { openPackage(app.getString("package")) }
            }
            if (index == 0) firstDock = card
            dock.addView(card, dockParams())
        }
        dock.addView(LauncherCard(this, "Roku", "HDMI 1").apply { id = View.generateViewId(); nextFocusUpId = FEATURED_ID; setArtwork(getDrawable(R.drawable.ic_hdmi_input)); setOnClickListener { inputSwitcher?.switchTo(config.getString("rokuInputId")) } }, dockParams())
        dock.addView(LauncherCard(this, "⋯", "Todos os apps").apply { id = View.generateViewId(); nextFocusUpId = FEATURED_ID; setArtwork(getDrawable(R.drawable.ic_grid)); setOnClickListener { showAllApps() } }, dockParams())
        featured.nextFocusDownId = firstDock?.id ?: View.NO_ID
        root.addView(dock, LinearLayout.LayoutParams(-1, px(108)))
        return root
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
