package com.example.conversormoedas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.conversormoedas.data.Wallet
import com.example.conversormoedas.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGoToConversion.setOnClickListener {
            startActivity(Intent(this, ConversionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val brLocale = Locale("pt", "BR")
        
        val brlFormat = NumberFormat.getCurrencyInstance(brLocale)
        binding.tvBrlBalance.text = brlFormat.format(Wallet.brlBalance)

        val usdFormat = NumberFormat.getCurrencyInstance(brLocale)
        binding.tvUsdBalance.text = usdFormat.format(Wallet.usdBalance).replace("R$", "$")

        val btcSymbols = DecimalFormatSymbols(brLocale)
        val btcFormat = DecimalFormat("#,##0.000000", btcSymbols)
        binding.tvBtcBalance.text = "${btcFormat.format(Wallet.btcBalance)} BTC"
    }
}
