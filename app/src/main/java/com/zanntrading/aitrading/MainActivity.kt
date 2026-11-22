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
    private lateinit var signalInfo: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private val updateTask = object : Runnable {
        override fun run() {
            fetchMarketPrices()
            handler.postDelayed(this, 15000)
        }
    }

    private val candleUpdateTask = object : Runnable {
        override fun run() {
            fetchCandles()
            fetchMultiTimeframe()
            handler.postDelayed(this, 60000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
        handler.post(updateTask)
        handler.post(candleUpdateTask)
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

    private fun fetchGoldPrice(): String {
        val url = URL("https://xaus.com/api/v1/spot")
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

            val json = JSONObject(response)
            return json.getJSONObject("xau").getString("price")
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchMarketPrices() {
        executor.execute {
            var btc = "Tidak tersedia"
            var eth = "Tidak tersedia"
            var gold = "Tidak tersedia"

            try {
                btc = fetchPrice("BTCUSDT")
                eth = fetchPrice("ETHUSDT")
            } catch (e: Exception) {
                android.util.Log.e("AI_TRADING", "Binance error", e)
            }

            try {
                gold = fetchGoldPrice()
            } catch (e: Exception) {
                android.util.Log.e("AI_TRADING", "GOLD error", e)
            }

            val btcOk = btc != "Tidak tersedia"
            val ethOk = eth != "Tidak tersedia"
            val goldOk = gold != "Tidak tersedia"

            runOnUiThread {
                marketInfo.text =
                    "Status: ${if (btcOk && ethOk && goldOk) "TERHUBUNG" else "SEBAGIAN TERHUBUNG"}" +
                    "\nBTC/USDT: $$btc" +
                    "\nETH/USDT: $$eth" +
                    "\nXAU/USD (GOLD): $$gold" +
                    "\nSumber GOLD: xaus.com" +
                    "\nUpdate otomatis: 15 detik"

                marketInfo.setTextColor(if (btcOk || ethOk || goldOk) green else red)
            }
        }
    }
    private fun fetchMultiTimeframe() {
        executor.execute {
            try {
                val intervals = listOf(
                    "1 MENIT" to "1m",
                    "5 MENIT" to "5m",
                    "15 MENIT" to "15m",
                    "1 JAM" to "1h"
                )

                val analyses = mutableListOf<Pair<String, SignalAnalysis>>()

                for ((label, interval) in intervals) {
                    val url = URL(
                        "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=$interval&limit=60"
                    )

                    val connection = url.openConnection() as HttpURLConnection

                    val candles = try {
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        if (connection.responseCode != 200) {
                            throw Exception("HTTP ${connection.responseCode}")
                        }

                        val response = connection.inputStream.bufferedReader().use {
                            it.readText()
                        }

                        val array = org.json.JSONArray(response)
                        val result = mutableListOf<Candle>()

                        for (i in 0 until array.length()) {
                            val item = array.getJSONArray(i)
                            result.add(
                                Candle(
                                    item.getString(1).toDouble(),
                                    item.getString(2).toDouble(),
                                    item.getString(3).toDouble(),
                                    item.getString(4).toDouble()
                                )
                            )
                        }

                        result
                    } finally {
                        connection.disconnect()
                    }

                    analyses.add(
                        label to TechnicalIndicators.calculate(candles)
                    )
                }

                val buyCount = analyses.count { it.second.recommendation == "BUY" }
                val sellCount = analyses.count { it.second.recommendation == "SELL" }

                val overall = when {
                    buyCount >= 3 -> "BUY"
                    sellCount >= 3 -> "SELL"
                    else -> "HOLD"
                }

                val report = buildString {
                    appendLine("BTC/USDT MULTI-TIMEFRAME")
                    appendLine()

                    for ((label, result) in analyses) {
                        appendLine(
                            "$label: ${result.recommendation} (${result.score}%)"
                        )
                    }

                    appendLine()
                    appendLine("BUY: $buyCount dari 4 timeframe")
                    appendLine("SELL: $sellCount dari 4 timeframe")
                    appendLine("KESIMPULAN GABUNGAN: $overall")
                }

                runOnUiThread {
                    multiTimeframeInfo.text = report
                    multiTimeframeInfo.setTextColor(
                        when (overall) {
                            "BUY" -> green
                            "SELL" -> red
                            else -> white
                        }
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    multiTimeframeInfo.text =
                        "Analisis multi-timeframe gagal: ${e.message}"
                }
            }
        }
    }

    private fun fetchCandles() {
        executor.execute {
            try {
                val url = URL(
                    "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=60"
                )
                val connection = url.openConnection() as HttpURLConnection

                val data = try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode != 200) {
                        throw Exception("HTTP ${connection.responseCode}")
                    }

                    val response = connection.inputStream.bufferedReader().use {
                        it.readText()
                    }

                    val array = org.json.JSONArray(response)
                    val result = mutableListOf<Candle>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONArray(i)
                        result.add(
                            Candle(
                                item.getString(1).toDouble(),
                                item.getString(2).toDouble(),
                                item.getString(3).toDouble(),
                                item.getString(4).toDouble()
                            )
                        )
                    }

                    result
                } finally {
                    connection.disconnect()
                }

                runOnUiThread {
                    candleChart.setCandles(data)

                    try {
                        val result = TechnicalIndicators.calculate(data)
                        signalInfo.text = """
                            Rekomendasi: ${result.recommendation}
                            Skor kondisi: ${result.score}%
                            EMA 20: ${String.format(java.util.Locale.US, "%.2f", result.ema20)}
                            EMA 50: ${String.format(java.util.Locale.US, "%.2f", result.ema50)}
                            RSI 14: ${String.format(java.util.Locale.US, "%.2f", result.rsi)}
                            MACD: ${String.format(java.util.Locale.US, "%.4f", result.macd)}
                            MACD Signal: ${String.format(java.util.Locale.US, "%.4f", result.macdSignal)}
                        """.trimIndent()

                        signalInfo.setTextColor(
                            when (result.recommendation) {
                                "BUY" -> green
                                "SELL" -> red
                                else -> white
                            }
                        )
                    } catch (e: Exception) {
                        signalInfo.text = "Analisis belum tersedia: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Grafik gagal dimuat: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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

        panel("BTC/USDT CANDLESTICK", "60 candle Binance - interval 1 menit")

        candleChart = CandlestickChartView(this)
        content.addView(
            candleChart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
        )
        panel(
            "XAUUSD / GOLD",
            "Status: Menunggu koneksi MT5"
        )

        val signalBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(card)
            setPadding(12, 12, 12, 12)
        }

        signalBox.addView(text("AI SIGNAL", 18f, green))
        signalInfo = text("Menunggu data Binance untuk analisis...", 14f, white)
        signalBox.addView(signalInfo)

        val signalParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        signalParams.setMargins(0, 8, 0, 8)
        content.addView(signalBox, signalParams)

        val multiBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(card)
            setPadding(12, 12, 12, 12)
        }

        multiBox.addView(text("MULTI-TIMEFRAME ANALYSIS", 18f, green))
        multiTimeframeInfo = text(
            "Menunggu analisis 1m, 5m, 15m, dan 1h...",
            14f,
            white
        )
        multiBox.addView(multiTimeframeInfo)

        val multiParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        multiParams.setMargins(0, 8, 0, 8)
        content.addView(multiBox, multiParams)

        fetchCandles()

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
        handler.removeCallbacks(candleUpdateTask)
        executor.shutdownNow()
        super.onDestroy()
    }
}
