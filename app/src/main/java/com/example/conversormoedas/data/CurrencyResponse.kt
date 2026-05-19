package com.example.conversormoedas.data

import com.google.gson.annotations.SerializedName

data class CurrencyItem(
    val code: String,
    val codein: String,
    val name: String,
    val bid: String
)

typealias AwesomeApiResponse = Map<String, CurrencyItem>
