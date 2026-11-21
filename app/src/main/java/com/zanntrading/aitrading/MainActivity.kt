package com.zanntrading.aitrading

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val bg = Color.rgb(12, 17, 28)
    private val card = Color.rgb(25, 34, 49)
    private val green = Color.rgb(0, 210, 140)
    private val red = Color.rgb(255, 85, 100)
    private val white = Color.WHITE
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
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
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        panel("DASHBOARD", "Status: AI Trading siap digunakan\nMode: Analisis dan persetujuan manual")
        panel("MARKET WATCH", "XAUUSD / GOLD: Menunggu koneksi MT5\nBTCUSDT: Menunggu data Binance")
        panel("AI SIGNAL", "Belum ada sinyal aktif\nSinyal akan muncul setelah data pasar tersedia")

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val buy = Button(this).apply {
            text = "BUY"
            setTextColor(white)
            setBackgroundColor(green)
            setOnClickListener {
                Toast.makeText(this@MainActivity,
                    "BUY: menunggu persetujuan dan integrasi broker",
                    Toast.LENGTH_LONG).show()
            }
        }

        val sell = Button(this).apply {
            text = "SELL"
            setTextColor(white)
            setBackgroundColor(red)
            setOnClickListener {
                Toast.makeText(this@MainActivity,
                    "SELL: menunggu persetujuan dan integrasi broker",
                    Toast.LENGTH_LONG).show()
            }
        }

        actions.addView(buy, LinearLayout.LayoutParams(0, 60, 1f))
        actions.addView(sell, LinearLayout.LayoutParams(0, 60, 1f))
        content.addView(actions)

        panel("ADMIN CONTROL", "Login admin: belum diaktifkan\nKontrol sistem: mode demo")
        panel("PROFIT / LOSS", "Belum ada transaksi\nRiwayat akan tersedia setelah integrasi")
        panel("CHART & MARKET", "Grafik pasar akan ditampilkan setelah koneksi data")
        panel("CAPITAL MANAGEMENT", "Modal: belum diatur\nManajemen risiko: belum aktif")
        panel("AUTO TRADING", "Status: NONAKTIF\nEksekusi otomatis dikunci")
    }
}
