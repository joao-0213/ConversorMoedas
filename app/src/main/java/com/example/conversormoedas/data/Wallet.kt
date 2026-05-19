package com.example.conversormoedas.data

import java.math.BigDecimal

object Wallet {
    var brlBalance: BigDecimal = BigDecimal("100000.00")
    var usdBalance: BigDecimal = BigDecimal("50000.00")
    var btcBalance: BigDecimal = BigDecimal("0.500000")

    fun getBalance(currency: String): BigDecimal {
        return when (currency) {
            "BRL" -> brlBalance
            "USD" -> usdBalance
            "BTC" -> btcBalance
            else -> BigDecimal.ZERO
        }
    }

    fun updateBalance(currency: String, amount: BigDecimal) {
        when (currency) {
            "BRL" -> brlBalance = brlBalance.add(amount)
            "USD" -> usdBalance = usdBalance.add(amount)
            "BTC" -> btcBalance = btcBalance.add(amount)
        }
    }
}
