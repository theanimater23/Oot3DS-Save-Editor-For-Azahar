package com.oot3d.saveeditor

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts the save-editor web UI (assets/editor.html, the same HTML/JS used in the
 * browser version) inside a WebView, and does real file I/O with the Storage
 * Access Framework instead of the browser's download/upload dance:
 *  - "Open save file" launches the system document picker once. After that the
 *    picked file's URI permission is persisted, so the app can find and reopen
 *    the exact same file automatically the next time it's launched.
 *  - "Write changes" in the web UI writes the edited bytes straight back to
 *    that same file in place - no renaming, no manual copy-over.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fileLabel: TextView
    private val prefs by lazy { getSharedPreferences("oot3d_save_editor", MODE_PRIVATE) }

    private var currentUri: Uri? = null
    private var currentName: String = ""
    private var pendingBytes: ByteArray? = null // bytes waiting for the page to ask for them

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openPickedFile(uri, persist = true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fileLabel = findViewById(R.id.fileLabel)
        webView = findViewById(R.id.webView)
        findViewById<View>(R.id.openButton).setOnClickListener { launchPicker() }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // If a file was already loaded (auto-reopened before the page finished
                // loading), let the page know it's ready to pull the bytes now.
                if (pendingBytes != null) webView.evaluateJavascript("window.onFileReady && window.onFileReady();", null)
            }
        }
        webView.addJavascriptInterface(Bridge(), "AndroidBridge")
        webView.loadUrl("file:///android_asset/editor.html")

        tryAutoReopenLastFile()
    }

    private fun launchPicker() {
        // No MIME restriction: OoT3D save files have no fixed extension across every
        // export tool, so a whitelist would just get in the way. Any single file works.
        openDocument.launch(arrayOf("*/*"))
    }

    private fun tryAutoReopenLastFile() {
        val saved = prefs.getString("last_uri", null) ?: return
        val uri = Uri.parse(saved)
        val stillGranted = contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        if (stillGranted) {
            openPickedFile(uri, persist = false)
        } else {
            fileLabel.text = "No file open yet — tap \u201cOpen Save File\u201d"
        }
    }

    private fun openPickedFile(uri: Uri, persist: Boolean) {
        try {
            if (persist) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Could not open that file for reading.")
            currentUri = uri
            currentName = queryDisplayName(uri)
            pendingBytes = bytes
            prefs.edit().putString("last_uri", uri.toString()).apply()
            fileLabel.text = currentName
            webView.evaluateJavascript("window.onFileReady && window.onFileReady();", null)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open that file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "save file"
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx)?.let { name = it }
                }
            }
        } catch (_: Exception) { }
        return name
    }

    /** Exposed to the page's JavaScript as `window.AndroidBridge`. */
    inner class Bridge {

        @JavascriptInterface
        fun hasFile(): Boolean = pendingBytes != null

        @JavascriptInterface
        fun getFileName(): String = currentName

        @JavascriptInterface
        fun getFileBytesBase64(): String {
            val b = pendingBytes ?: return ""
            return Base64.encodeToString(b, Base64.NO_WRAP)
        }

        /** Called from the web UI's "Write changes" button. Writes straight back
         *  into the same file the person picked - in place, no new file created. */
        @JavascriptInterface
        fun saveFileBase64(base64: String): Boolean {
            val uri = currentUri ?: return false
            return try {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                // "wt" truncates before writing so a smaller-than-before write (shouldn't
                // happen here since edits never change file length) can't leave stale bytes.
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                    ?: return false
                pendingBytes = bytes
                runOnUiThread { Toast.makeText(this@MainActivity, "Saved to $currentName", Toast.LENGTH_SHORT).show() }
                true
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Couldn't save: ${e.message}", Toast.LENGTH_LONG).show() }
                false
            }
        }

        @JavascriptInterface
        fun requestOpenFile() {
            runOnUiThread { launchPicker() }
        }

        @JavascriptInterface
        fun toast(msg: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
        }
    }
}
