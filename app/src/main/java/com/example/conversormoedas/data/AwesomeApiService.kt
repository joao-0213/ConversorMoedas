package com.example.conversormoedas.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApiService {
    @GET("last/{pair}")
    suspend fun getExchangeRate(@Path("pair") pair: String): Response<AwesomeApiResponse>
}
