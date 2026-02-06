package com.example.eulcauink

import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    // Store content pending export until the user selects a file location
    private var pendingExportContent: String? = null

    // --- 1. Image Picker Launcher ---
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filename = copyContentToInternal(uri, "images", "img_${System.currentTimeMillis()}.png")
            if (filename != null) {
                sendJsEvent("PICK_IMAGE_RESULT", filename)
            } else {
                sendJsEvent("ERROR", "Failed to copy image to internal storage")
            }
        }
    }

    // --- 2. Markdown Import Launcher ---
    private val importMdLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                // Read file content
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader().use { it?.readText() } ?: ""

                // Try to get filename
                var filename = "imported.md"
                val cursor = contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor != null && nameIndex != null && nameIndex >= 0 && cursor.moveToFirst()) {
                    filename = cursor.getString(nameIndex)
                    cursor.close()
                }

                // Send back to JS
                // We use Gson to escape the content string safely
                val jsonContent = Gson().toJson(content)
                // Remove surrounding quotes from Gson output as we are passing it as a string argument
                // Actually, passing jsonContent directly as the second argument is safer if we treat it as a string literal in JS
                // But simpler approach:
                val script = "window.handleAndroidEvent('IMPORT_MD_RESULT', $jsonContent, '$filename')"
                webView.evaluateJavascript(script, null)

            } catch (e: Exception) {
                sendJsEvent("ERROR", "Import failed: ${e.message}")
            }
        }
    }

    // --- 3. Markdown Export Launcher ---
    private val exportMdLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            try {
                contentResolver.openOutputStream(uri)?.use {
                    it.write(pendingExportContent!!.toByteArray())
                }
                sendJsEvent("EXPORT_SUCCESS", uri.path ?: "Saved")
            } catch (e: Exception) {
                sendJsEvent("ERROR", "Export failed: ${e.message}")
            }
        }
        pendingExportContent = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize WebView programmatically
        webView = WebView(this)
        setContentView(webView)

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        // Configure AssetLoader
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("eulcauink.local")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/user-images/", WebViewAssetLoader.InternalStoragePathHandler(this, imagesDir))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false // Security best practice, use AssetLoader
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        WebView.setWebContentsDebuggingEnabled(true)

        // Pass reference of Activity to Interface so it can trigger launchers
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.loadUrl("https://eulcauink.local/assets/index.html")

        // --- Handle Back Navigation (Modern Approach) ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Disable this callback and propagate the back press to the system (exit app)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // --- Public methods called by WebAppInterface ---

    fun launchPickImage() {
        pickImageLauncher.launch("image/*")
    }

    fun launchImportMarkdown() {
        // MIME types for markdown can be tricky, allow text/* and generic
        importMdLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*"))
    }

    fun launchExportMarkdown(content: String, filename: String) {
        pendingExportContent = content
        exportMdLauncher.launch(filename)
    }

    private fun sendJsEvent(type: String, data: String) {
        runOnUiThread {
            webView.evaluateJavascript("window.handleAndroidEvent('$type', '$data')", null)
        }
    }

    private fun copyContentToInternal(uri: Uri, dirName: String, outputFilename: String): String? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val dir = File(filesDir, dirName)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, outputFilename)

            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            return outputFilename
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
