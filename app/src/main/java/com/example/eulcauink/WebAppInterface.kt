package com.example.eulcauink

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.net.toUri
import com.google.gson.Gson
import java.io.File


class WebAppInterface(private val context: Context) {

    private val notesDir: File = File(context.filesDir, "notes").apply {
        if (!exists()) mkdirs()
    }

    private val imagesDir: File = File(context.filesDir, "images").apply {
        if (!exists()) mkdirs()
    }

    // ===== Images =====

    @JavascriptInterface
    fun saveImage(base64Data: String, filename: String): String {
        val cleanBase64 = base64Data.substringAfter(",")
        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

        val file = File(imagesDir, filename)
        file.writeBytes(bytes)

        return filename
    }

    // ===== Notes =====

    @JavascriptInterface
    fun saveNote(filename: String, content: String) {
        val file = File(notesDir, filename)
        file.writeText(content)
    }

    @JavascriptInterface
    fun loadNote(filename: String): String {
        val file = File(notesDir, filename)
        return if (file.exists()) file.readText() else ""
    }

    @JavascriptInterface
    fun getNoteList(): String {
        val list = notesDir.listFiles()
            ?.filter { it.extension == "md" }
            ?.map {
                mapOf(
                    "filename" to it.name,
                    "title" to it.nameWithoutExtension,
                    "updatedAt" to it.lastModified()
                )
            } ?: emptyList()

        return Gson().toJson(list)
    }

    @JavascriptInterface
    fun deleteNote(filename: String): Boolean {
        val file = File(notesDir, filename)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    @JavascriptInterface
    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // --- System Actions ---

    @JavascriptInterface
    fun openExternalLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open link: " + e.message)
        }
    }
}
