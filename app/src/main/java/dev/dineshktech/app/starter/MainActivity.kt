package com.dkdevpro.androidappstarter  // این رو به پکیج خودت تغییر بده

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var tokenInput: EditText
    private lateinit var tailscaleInput: EditText
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var copyBtn: Button
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentSSH = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createLayout())
    }

    private fun createLayout(): View {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF0D1117.toInt())
            isFillViewport = true
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 40, 40, 40)
        }

        // ========== هدر ==========
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val emojiText = TextView(this).apply {
            text = "🚀"
            textSize = 32f
        }
        headerLayout.addView(emojiText)

        val titleText = TextView(this).apply {
            text = " VPS One-Click"
            textSize = 24f
            setTextColor(0xFFF0F6FC.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        headerLayout.addView(titleText)
        mainLayout.addView(headerLayout)

        // ========== فیلد توکن ==========
        tokenInput = EditText(this).apply {
            hint = "🔑 توکن گیت‌هاب"
            setHintTextColor(0xFF8B949E.toInt())
            setTextColor(0xFFF0F6FC.toInt())
            setBackgroundColor(0xFF161B22.toInt())
            setPadding(40, 20, 40, 20)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
            ).apply { setMargins(0, 0, 0, 20) }
        }
        mainLayout.addView(tokenInput)

        // ========== فیلد کلید Tailscale ==========
        tailscaleInput = EditText(this).apply {
            hint = "🔗 کلید Tailscale"
            setHintTextColor(0xFF8B949E.toInt())
            setTextColor(0xFFF0F6FC.toInt())
            setBackgroundColor(0xFF161B22.toInt())
            setPadding(40, 20, 40, 20)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
            ).apply { setMargins(0, 0, 0, 20) }
        }
        mainLayout.addView(tailscaleInput)

        // ========== دکمه یک‌کلیک ==========
        val oneClickBtn = Button(this).apply {
            text = "⚡ یک‌کلیک VPS"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor(0xFF2EA043.toInt())
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            ).apply { setMargins(0, 0, 0, 20) }
            setOnClickListener { startOneClick() }
        }
        mainLayout.addView(oneClickBtn)

        // ========== وضعیت ==========
        statusText = TextView(this).apply {
            text = "⏳ آماده برای شروع..."
            setTextColor(0xFF8B949E.toInt())
            setBackgroundColor(0xFF161B22.toInt())
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
        }
        mainLayout.addView(statusText)

        // ========== نتیجه ==========
        resultText = TextView(this).apply {
            text = ""
            setTextColor(0xFFF0F6FC.toInt())
            setBackgroundColor(0xFF161B22.toInt())
            setPadding(30, 20, 30, 20)
            minHeight = 100
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
        }
        mainLayout.addView(resultText)

        // ========== دکمه کپی ==========
        copyBtn = Button(this).apply {
            text = "📋 کپی لینک SSH"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF30363D.toInt())
            setPadding(20, 20, 20, 20)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
            ).apply { setMargins(0, 10, 0, 0) }
            setOnClickListener { copySSH() }
        }
        mainLayout.addView(copyBtn)

        scrollView.addView(mainLayout)
        return scrollView
    }

    private fun startOneClick() {
        val token = tokenInput.text.toString().trim()
        val tailscaleKey = tailscaleInput.text.toString().trim()

        if (token.isEmpty()) {
            showToast("لطفاً توکن گیت‌هاب را وارد کنید")
            return
        }
        if (tailscaleKey.isEmpty()) {
            showToast("لطفاً کلید Tailscale را وارد کنید")
            return
        }

        statusText.text = "⚡ شروع فرآیند یک‌کلیک..."
        resultText.text = ""
        copyBtn.visibility = View.GONE

        executor.execute {
            try {
                val repoName = "VPS-${System.currentTimeMillis() / 1000}"
                updateStatus("📁 ایجاد مخزن: $repoName")
                val repo = createRepo(token, repoName)

                updateStatus("🔑 تنظیم Secret...")
                setupTailscaleSecret(token, repo, tailscaleKey)

                updateStatus("📂 ایجاد workflow...")
                createWorkflow(token, repo)

                updateStatus("🚀 اجرای VPS...")
                val ssh = runWorkflow(token, repo)

                mainHandler.post {
                    statusText.text = "✅ VPS آماده است! 🎉"
                    resultText.text = "🔑 لینک اتصال SSH:\n\n$ssh"
                    currentSSH = ssh
                    copyBtn.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                mainHandler.post {
                    statusText.text = "❌ خطا: ${e.message}"
                }
            }
        }
    }

    private fun createRepo(token: String, name: String): String {
        val url = URL("https://api.github.com/user/repos")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "token $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val json = "{\"name\":\"$name\",\"private\":false}"
        DataOutputStream(conn.outputStream).use { it.writeBytes(json) }
        if (conn.responseCode == 201) {
            val response = readResponse(conn)
            return JSONObject(response).getString("full_name")
        } else {
            throw Exception("خطا در ایجاد مخزن: ${conn.responseCode}")
        }
    }

    private fun setupTailscaleSecret(token: String, repo: String, key: String) {
        val url = URL("https://api.github.com/repos/$repo/actions/secrets/TAILSCALE_AUTHKEY")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Authorization", "token $token")
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val encoded = android.util.Base64.encodeToString(key.toByteArray(), android.util.Base64.NO_WRAP)
        val json = "{\"encrypted_value\":\"$encoded\",\"key_id\":\"\"}"
        DataOutputStream(conn.outputStream).use { it.writeBytes(json) }
        val code = conn.responseCode
        if (code != 201 && code != 204) {
            throw Exception("خطا در تنظیم Secret: $code")
        }
    }

    private fun createWorkflow(token: String, repo: String) {
        val workflow = "name: VPS Creator\non:\n  workflow_dispatch:\njobs:\n  vps:\n    runs-on: ubuntu-latest\n    steps:\n      - name: Checkout\n        uses: actions/checkout@v4\n      - name: Start tmate\n        uses: mxschmitt/action-tmate@v3"
        val encoded = android.util.Base64.encodeToString(workflow.toByteArray(), android.util.Base64.NO_WRAP)
        val url = URL("https://api.github.com/repos/$repo/contents/.github/workflows/vps.yml")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Authorization", "token $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val json = "{\"message\":\"Add VPS workflow\",\"content\":\"$encoded\"}"
        DataOutputStream(conn.outputStream).use { it.writeBytes(json) }
        val code = conn.responseCode
        if (code != 201 && code != 200) {
            throw Exception("خطا در ایجاد workflow: $code")
        }
    }

    private fun runWorkflow(token: String, repo: String): String {
        val url = URL("https://api.github.com/repos/$repo/actions/workflows/vps.yml/dispatches")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "token $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val json = "{\"ref\":\"main\"}"
        DataOutputStream(conn.outputStream).use { it.writeBytes(json) }
        if (conn.responseCode == 204) {
            return "✅ VPS workflow با موفقیت اجرا شد!\n📋 لینک SSH در لاگ‌های Actions ظاهر می‌شود.\n🔗 مخزن: $repo"
        } else {
            throw Exception("خطا در اجرای workflow: ${conn.responseCode}")
        }
    }

    private fun readResponse(conn: HttpURLConnection): String {
        BufferedReader(InputStreamReader(conn.inputStream)).use {
            return it.readText()
        }
    }

    private fun updateStatus(msg: String) {
        mainHandler.post { statusText.text = msg }
    }

    private fun copySSH() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SSH", currentSSH)
        clipboard.setPrimaryClip(clip)
        showToast("📋 لینک SSH کپی شد!")
    }

    private fun showToast(msg: String) {
        mainHandler.post { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }
}
