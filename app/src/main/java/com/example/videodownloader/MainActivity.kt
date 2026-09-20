package com.example.videodownloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var btnDownload: Button
    private var detectedVideoUrl: String? = null

    private val adDomains = listOf(
        "doubleclick.net", "googlesyndication.com", "popads.net",
        "popcash.net", "adsterra.com", "exoclick.com", "juicyads.com",
        "bet365.com", "adform.net", "yieldmanager.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        urlInput = EditText(this).apply {
            hint = "أدخل رابطاً أو ابحث في جوجل..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnGo = Button(this).apply {
            text = "ذهاب"
            setOnClickListener { loadUrl(urlInput.text.toString()) }
        }
        topBar.addView(urlInput)
        topBar.addView(btnGo)

        btnDownload = Button(this).apply {
            text = "⚡ تم التقاط فيديو! اضغط للتحميل"
            visibility = View.GONE
            setBackgroundColor(android.graphics.Color.parseColor("#22C55E"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { triggerDownload() }
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
        }

        rootLayout.addView(topBar)
        rootLayout.addView(btnDownload)
        rootLayout.addView(webView)
        setContentView(rootLayout)

        setupAdvancedBrowser()

        webView.loadUrl("https://www.google.com")
    }

    private fun setupAdvancedBrowser() {
        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()?.lowercase(Locale.ROOT) ?: return null

                for (adDomain in adDomains) {
                    if (url.contains(adDomain)) {
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }

                if (url.contains(".mp4") || url.contains(".m3u8") || url.contains(".webm") || url.contains("video/")) {
                    if (!url.contains("analytics") && !url.contains("telemetry")) {
                        detectedVideoUrl = request.url.toString()
                        runOnUiThread {
                            btnDownload.visibility = View.VISIBLE
                        }
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                urlInput.setText(url)
            }
        }
    }

    private fun loadUrl(input: String) {
        var url = input.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://www.google.com/search?q=$url"
        }
        detectedVideoUrl = null
        btnDownload.visibility = View.GONE
        webView.loadUrl(url)
    }

    private fun triggerDownload() {
        val videoUrl = detectedVideoUrl ?: return

        val builder = AlertDialog.Builder(this)
        builder.setTitle("تحميل الفيديو")
        builder.setMessage("هل تريد بدء تحميل الفيديو المكتشف؟")
        builder.setPositiveButton("تحميل") { _, _ ->
            val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
                setTitle("جاري تحميل الفيديو...")
                setDescription("التطبيق يقوم بتنزيل الملف الآن.")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Video_${System.currentTimeMillis()}.mp4")
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "بدأ التحميل في الخلفية!", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
