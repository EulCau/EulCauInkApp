package com.example.eulcauink

import android.app.AlertDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var pendingExportContent: String? = null

    // 1. Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filename = copyContentToInternal(uri, "images", "img_${System.currentTimeMillis()}.png")
            if (filename != null) sendJsEvent("PICK_IMAGE_RESULT", filename)
            else sendJsEvent("ERROR", "Failed to copy image")
        }
    }

    // 2. Import
    private val importMdLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                var filename = "imported.md"
                val cursor = contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor != null && nameIndex != null && nameIndex >= 0 && cursor.moveToFirst()) {
                    filename = cursor.getString(nameIndex)
                    cursor.close()
                }
                val jsonContent = Gson().toJson(content)
                val script = "window.handleAndroidEvent('IMPORT_MD_RESULT', $jsonContent, '$filename')"
                webView.evaluateJavascript(script, null)
            } catch (e: Exception) {
                sendJsEvent("ERROR", "Import failed: ${e.message}")
            }
        }
    }

    // 3. Export
    private val exportMdLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(pendingExportContent!!.toByteArray()) }
                sendJsEvent("EXPORT_SUCCESS", uri.path ?: "Saved")
            } catch (e: Exception) {
                sendJsEvent("ERROR", "Export failed: ${e.message}")
            }
        }
        pendingExportContent = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

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
            allowFileAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        WebView.setWebContentsDebuggingEnabled(true)
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        // --- Long Press to Save Image Logic ---
        webView.setOnLongClickListener { view ->
            val result = (view as WebView).hitTestResult
            val type = result.type

            if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val imageUrl = result.extra
                if (imageUrl != null) {
                    showSaveImageDialog(imageUrl)
                    return@setOnLongClickListener true // Consumes the event
                }
            }
            false
        }

        webView.loadUrl("https://eulcauink.local/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
    }

    private fun showSaveImageDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Image Options")
            .setMessage("Save image to Gallery (EulCauInkPic)?")
            .setPositiveButton("Save") { _, _ ->
                // Do network/IO on background thread
                Thread {
                    saveImageToGallery(url)
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveImageToGallery(url: String) {
        try {
            var inputStream: InputStream? = null
            var filename = "img_${System.currentTimeMillis()}.png"

            // 1. Handle Virtual Local Domain (App internal storage)
            // URL looks like: https://eulcauink.local/user-images/drawing_123.png
            if (url.contains("eulcauink.local/user-images/")) {
                val extractedName = url.substringAfterLast("/")
                val localFile = File(filesDir, "images/$extractedName")
                if (localFile.exists()) {
                    inputStream = FileInputStream(localFile)
                    filename = extractedName
                }
            }
            // 2. Handle Base64
            else if (url.startsWith("data:image")) {
                val base64Data = url.substringAfter(",")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                inputStream = bytes.inputStream()
            }
            // 3. Handle External Network URL
            else if (url.startsWith("http")) {
                inputStream = URL(url).openStream()
            }

            if (inputStream == null) {
                runOnUiThread { Toast.makeText(this, "Failed to load image source", Toast.LENGTH_SHORT).show() }
                return
            }

            // Write to MediaStore (Public Gallery)
            val folderName = "EulCauInkPic"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png") // Assuming PNG for simplicity, could detect
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + folderName)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { out ->
                    inputStream.use { input ->
                        input.copyTo(out)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                runOnUiThread { Toast.makeText(this, "Saved to Pictures/$folderName", Toast.LENGTH_SHORT).show() }
            } else {
                runOnUiThread { Toast.makeText(this, "Failed to create gallery entry", Toast.LENGTH_SHORT).show() }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun launchPickImage() { pickImageLauncher.launch("image/*") }
    fun launchImportMarkdown() { importMdLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*")) }
    fun launchExportMarkdown(content: String, filename: String) {
        pendingExportContent = content
        exportMdLauncher.launch(filename)
    }

    private fun sendJsEvent(type: String, data: String) {
        runOnUiThread { webView.evaluateJavascript("window.handleAndroidEvent('$type', '$data')", null) }
    }

    private fun copyContentToInternal(uri: Uri, dirName: String, outputFilename: String): String? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val dir = File(filesDir, dirName)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, outputFilename)
            FileOutputStream(file).use { inputStream.copyTo(it) }
            return outputFilename
        } catch (_: Exception) { return null }
    }
}
