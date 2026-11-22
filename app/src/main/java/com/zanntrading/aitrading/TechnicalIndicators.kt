package com.zanntrading.aitrading

data class SignalAnalysis(
    val ema20: Double,
    val ema50: Double,
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val recommendation: String,
    val score: Int
)

object TechnicalIndicators {

    private fun ema(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()

        val multiplier = 2.0 / (period + 1)
        val result = mutableListOf<Double>()
        var previous = values.first()

        result.add(previous)

        for (i in 1 until values.size) {
            previous = (values[i] - previous) * multiplier + previous
            result.add(previous)
        }

        return result
    }

    fun calculate(candles: List<Candle>): SignalAnalysis {
        require(candles.size >= 50) {
            "Minimal diperlukan 50 candle"
        }

        val closes = candles.map { it.close }

        require(closes.all { it.isFinite() && it > 0.0 }) {
            "Data harga tidak valid"
        }

        val ema20Values = ema(closes, 20)
        val ema50Values = ema(closes, 50)
        val ema12 = ema(closes, 12)
        val ema26 = ema(closes, 26)

        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()

        for (i in closes.size - 14 until closes.size) {
            val change = closes[i] - closes[i - 1]
            gains.add(if (change > 0) change else 0.0)
            losses.add(if (change < 0) -change else 0.0)
        }

        val averageGain = gains.average()
        val averageLoss = losses.average()

        val rsi = when {
            averageLoss == 0.0 && averageGain == 0.0 -> 50.0
            averageLoss == 0.0 -> 100.0
            else -> 100.0 - (100.0 / (1.0 + averageGain / averageLoss))
        }

        val macdValues = closes.indices.map {
            ema12[it] - ema26[it]
        }

        val signalValues = ema(macdValues, 9)

        val latestEma20 = ema20Values.last()
        val latestEma50 = ema50Values.last()
        val latestMacd = macdValues.last()
        val latestSignal = signalValues.last()
        val latestClose = closes.last()

        val ema20Rising = latestEma20 > ema20Values[ema20Values.size - 6]
        val ema20Falling = latestEma20 < ema20Values[ema20Values.size - 6]

        val buyConditions = listOf(
            latestEma20 > latestEma50,
            latestClose > latestEma20,
            ema20Rising,
            rsi > 50.0 && rsi < 70.0,
            latestMacd > latestSignal
        )

        val sellConditions = listOf(
            latestEma20 < latestEma50,
            latestClose < latestEma20,
            ema20Falling,
            rsi < 50.0 && rsi > 30.0,
            latestMacd < latestSignal
        )

        val buyScore = buyConditions.count { it }
        val sellScore = sellConditions.count { it }

        val recommendation = when {
            buyConditions.first() && buyScore >= 4 -> "BUY"
            sellConditions.first() && sellScore >= 4 -> "SELL"
            else -> "HOLD"
        }

        val score = when (recommendation) {
            "BUY" -> buyScore * 20
            "SELL" -> sellScore * 20
            else -> maxOf(buyScore, sellScore) * 20
        }

        return SignalAnalysis(
            latestEma20,
            latestEma50,
            rsi,
            latestMacd,
            latestSignal,
            recommendation,
            score
        )
    }
}
