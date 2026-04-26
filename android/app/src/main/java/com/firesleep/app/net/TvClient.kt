package com.firesleep.app.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TvClient(private val baseUrl: String) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun health(): Result<Unit> = runCatching {
        val req = Request.Builder()
            .url("$baseUrl/health")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "health returned ${resp.code}" }
        }
    }

    fun powerOff(): Result<Unit> = runCatching {
        val req = Request.Builder()
            .url("$baseUrl/poweroff")
            .post("".toRequestBody(null))
            .build()
        http.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "poweroff returned ${resp.code}" }
        }
    }

    companion object {
        fun baseUrlFor(ip: String): String = "http://$ip:8765"
    }
}
