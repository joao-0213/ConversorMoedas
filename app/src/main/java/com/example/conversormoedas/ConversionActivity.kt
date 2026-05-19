package com.example.conversormoedas

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.conversormoedas.data.RetrofitClient
import com.example.conversormoedas.data.Wallet
import com.example.conversormoedas.databinding.ActivityConversionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class ConversionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversionBinding
    private val currencies = arrayOf("BRL", "USD", "BTC")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        binding.actvSourceCurrency.setAdapter(adapter)
        binding.actvTargetCurrency.setAdapter(adapter)

        binding.btnConvert.setOnClickListener {
            performConversion()
        }
    }

    private fun performConversion() {
        val from = binding.actvSourceCurrency.text.toString()
        val to = binding.actvTargetCurrency.text.toString()
        val amountStr = binding.etAmount.text.toString()

        if (from.isEmpty() || to.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (from == to) {
            Toast.makeText(this, "Selecione moedas diferentes", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try { BigDecimal(amountStr) } catch (e: Exception) { BigDecimal.ZERO }

        if (amount <= BigDecimal.ZERO) {
            Toast.makeText(this, "O valor deve ser maior que zero", Toast.LENGTH_SHORT).show()
            return
        }

        val currentBalance = Wallet.getBalance(from)
        if (currentBalance < amount) {
            Toast.makeText(this, "Saldo insuficiente em $from", Toast.LENGTH_SHORT).show()
            return
        }

        fetchRateAndConvert(from, to, amount)
    }

    private fun fetchRateAndConvert(from: String, to: String, amount: BigDecimal) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.btnConvert.isEnabled = false
        binding.tvResult.text = ""

        lifecycleScope.launch {
            try {
                delay(1500)
                val pair = "$from-$to"
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.getExchangeRate(pair)
                }

                if (response.isSuccessful && response.body() != null) {
                    val rateMap = response.body()!!
                    val key = pair.replace("-", "")
                    val rateStr = rateMap[key]?.bid ?: rateMap.values.firstOrNull()?.bid
                    
                    if (rateStr != null) {
                        val result = amount.multiply(BigDecimal(rateStr))
                        executeTransaction(from, to, amount, result)
                    } else {
                        tryReverseOrIndirect(from, to, amount)
                    }
                } else {
                    tryReverseOrIndirect(from, to, amount)
                }
            } catch (e: Exception) {
                showError("Erro de conexão")
            } finally {
                binding.loadingOverlay.visibility = View.GONE
                binding.btnConvert.isEnabled = true
            }
        }
    }

    private suspend fun tryReverseOrIndirect(from: String, to: String, amount: BigDecimal) {
        try {
            val reversePair = "$to-$from"
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.service.getExchangeRate(reversePair)
            }

            if (response.isSuccessful && response.body() != null) {
                val rateMap = response.body()!!
                val key = reversePair.replace("-", "")
                val rateStr = rateMap[key]?.bid ?: rateMap.values.firstOrNull()?.bid
                
                if (rateStr != null) {
                    val rate = BigDecimal(rateStr)
                    val result = amount.divide(rate, 10, RoundingMode.HALF_UP)
                    executeTransaction(from, to, amount, result)
                    return
                }
            }
            tryIndirectConversion(from, to, amount)
        } catch (e: Exception) {
            tryIndirectConversion(from, to, amount)
        }
    }

    private suspend fun tryIndirectConversion(from: String, to: String, amount: BigDecimal) {
        try {
            val respBtcUsd = withContext(Dispatchers.IO) { RetrofitClient.service.getExchangeRate("BTC-USD") }
            val respUsdBrl = withContext(Dispatchers.IO) { RetrofitClient.service.getExchangeRate("USD-BRL") }
            
            if (respBtcUsd.isSuccessful && respUsdBrl.isSuccessful) {
                val rateBtcUsd = BigDecimal(respBtcUsd.body()?.get("BTCUSD")?.bid ?: "1")
                val rateUsdBrl = BigDecimal(respUsdBrl.body()?.get("USDBRL")?.bid ?: "1")
                
                val result = when {
                    from == "BRL" && to == "BTC" -> amount.divide(rateUsdBrl, 10, RoundingMode.HALF_UP).divide(rateBtcUsd, 10, RoundingMode.HALF_UP)
                    from == "BTC" && to == "BRL" -> amount.multiply(rateBtcUsd).multiply(rateUsdBrl)
                    from == "USD" && to == "BTC" -> amount.divide(rateBtcUsd, 10, RoundingMode.HALF_UP)
                    from == "BTC" && to == "USD" -> amount.multiply(rateBtcUsd)
                    else -> null
                }

                if (result != null) {
                    executeTransaction(from, to, amount, result)
                } else {
                    showError("Par de moedas não suportado")
                }
            } else {
                showError("Erro ao buscar taxas de conversão")
            }
        } catch (e: Exception) {
            showError("Falha na operação de conversão")
        }
    }

    private fun executeTransaction(from: String, to: String, amount: BigDecimal, result: BigDecimal) {
        Wallet.updateBalance(from, amount.negate())
        Wallet.updateBalance(to, result)
        binding.tvResult.text = "Resultado: ${formatCurrency(result, to)}"
        Toast.makeText(this, "Sucesso!", Toast.LENGTH_SHORT).show()
    }

    private fun formatCurrency(value: BigDecimal, currency: String): String {
        val brLocale = Locale("pt", "BR")
        return when (currency) {
            "BRL" -> NumberFormat.getCurrencyInstance(brLocale).format(value)
            "USD" -> NumberFormat.getCurrencyInstance(brLocale).format(value).replace("R$", "$")
            "BTC" -> {
                val symbols = DecimalFormatSymbols(brLocale)
                val df = DecimalFormat("#,##0.000000", symbols)
                "${df.format(value.setScale(6, RoundingMode.HALF_UP))} BTC"
            }
            else -> value.toPlainString()
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
