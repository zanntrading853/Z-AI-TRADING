package com.zanntrading.aitrading

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import java.util.Locale

data class Candle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

class CandlestickChartView(context: Context) : View(context) {

    private var candles: List<Candle> = emptyList()

    private val green = Color.rgb(0, 210, 140)
    private val red = Color.rgb(255, 85, 100)

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 65, 80)
        strokeWidth = 1f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 25f
    }

    private val wickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setCandles(data: List<Candle>) {
        candles = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(18, 25, 38))

        if (candles.isEmpty()) {
            canvas.drawText(
                "Menunggu data candlestick...",
                25f,
                height / 2f,
                textPaint
            )
            return
        }

        val left = 25f
        val right = width - 25f
        val top = 35f
        val bottom = height - 45f

        val highest = candles.maxOf { it.high }
        val lowest = candles.minOf { it.low }
        val range = (highest - lowest).takeIf { it > 0.0 } ?: 1.0

        fun priceY(price: Double): Float {
            return bottom - ((price - lowest) / range).toFloat() * (bottom - top)
        }

        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4f
            canvas.drawLine(left, y, right, y, gridPaint)

            val price = highest - range * i / 4.0
            canvas.drawText(
                String.format(Locale.US, "%.2f", price),
                left,
                y - 5f,
                textPaint
            )
        }

        val chartWidth = right - left
        val candleWidth = chartWidth / candles.size
        val bodyWidth = (candleWidth * 0.65f).coerceAtLeast(2f)

        candles.forEachIndexed { index, candle ->
            val centerX = left + candleWidth * (index + 0.5f)
            val color = if (candle.close >= candle.open) green else red

            wickPaint.color = color
            bodyPaint.color = color

            canvas.drawLine(
                centerX,
                priceY(candle.high),
                centerX,
                priceY(candle.low),
                wickPaint
            )

            val openY = priceY(candle.open)
            val closeY = priceY(candle.close)

            val bodyTop = minOf(openY, closeY)
            val bodyBottom = maxOf(openY, closeY).coerceAtLeast(bodyTop + 2f)

            canvas.drawRect(
                centerX - bodyWidth / 2f,
                bodyTop,
                centerX + bodyWidth / 2f,
                bodyBottom,
                bodyPaint
            )
        }
    }
}
