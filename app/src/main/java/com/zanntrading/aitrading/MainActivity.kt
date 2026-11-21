package com.zanntrading.aitrading

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val bg = Color.rgb(12, 17, 28)
    private val card = Color.rgb(25, 34, 49)
    private val green = Color.rgb(0, 210, 140)
    private val red = Color.rgb(255, 85, 100)
    private val white = Color.WHITE

    private lateinit var content: LinearLayout
    private lateinit var marketInfo: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private val updateTask = object : Runnable {
        override fun run() {
            fetchMarketPrices()
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
        handler.post(updateTask)
    }

    private fun text(value: String, size: Float, color: Int): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(12, 12, 12, 12)
        }
    }

    private fun panel(title: String, detail: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(card)
            setPadding(12, 12, 12, 12)
        }

        box.addView(text(title, 18f, green))
        box.addView(text(detail, 14f, white))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        content.addView(box, params)
    }

    private fun marketPanel() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(card)
            setPadding(12, 12, 12, 12)
        }

        box.addView(text("BINANCE LIVE MARKET", 18f, green))

        marketInfo = text(
            "Menghubungkan ke Binance...\nBTC/USDT: Memuat...\nETH/USDT: Memuat...",
            15f,
            white
        )

        box.addView(marketInfo)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        content.addView(box, params)
    }

    private fun fetchPrice(symbol: String): String {
        val url = URL(
            "https://api.binance.com/api/v3/ticker/price?symbol=$symbol"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode != 200) {
                throw Exception("HTTP ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            return JSONObject(response).getString("price")
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchMarketPrices() {
        executor.execute {
            try {
                val btc = fetchPrice("BTCUSDT")
                val eth = fetchPrice("ETHUSDT")

                runOnUiThread {
                    marketInfo.text =
                        "Status: TERHUBUNG\n" +
                        "BTC/USDT: $$btc\n" +
                        "ETH/USDT: $$eth\n" +
                        "Update otomatis: 15 detik"

                    marketInfo.setTextColor(green)
                }

          } catch (e: Exception) {
    val error = "${e.javaClass.simpleName}: ${e.message ?: "Tidak ada detail"}"

    runOnUiThread {
        marketInfo.text =
            "Status: KONEKSI GAGAL\n" +
            "Penyebab: $error\n" +
            "BTC/USDT: Tidak tersedia\n" +
            "ETH/USDT: Tidak tersedia"

        marketInfo.setTextColor(red)
    }
}
                }
           }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = text("Z-AI TRADING", 25f, green)
        header.gravity = Gravity.CENTER
        root.addView(header)

        val scroll = ScrollView(this)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 16)
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        panel(
            "DASHBOARD",
            "Status: AI Trading siap digunakan\nMode: Analisis dan persetujuan manual"
        )

        marketPanel()

        panel(
            "XAUUSD / GOLD",
            "Status: Menunggu koneksi MT5"
        )

        panel(
            "AI SIGNAL",
            "Belum ada sinyal aktif\nMenunggu data pasar untuk analisis"
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val buy = Button(this).apply {
            text = "BUY"
            setTextColor(white)
            setBackgroundColor(green)
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "BUY belum terhubung ke broker",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val sell = Button(this).apply {
            text = "SELL"
            setTextColor(white)
            setBackgroundColor(red)
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "SELL belum terhubung ke broker",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        actions.addView(buy, LinearLayout.LayoutParams(0, 60, 1f))
        actions.addView(sell, LinearLayout.LayoutParams(0, 60, 1f))
        content.addView(actions)

        panel("ADMIN CONTROL", "Login admin: belum diaktifkan")
        panel("PROFIT / LOSS", "Belum ada transaksi")
        panel("CHART & MARKET", "Data harga Binance tersedia di panel Live Market")
        panel("CAPITAL MANAGEMENT", "Modal: belum diatur")
        panel("AUTO TRADING", "Status: NONAKTIF\nEksekusi otomatis dikunci")
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateTask)
        executor.shutdownNow()
        super.onDestroy()
    }
}
