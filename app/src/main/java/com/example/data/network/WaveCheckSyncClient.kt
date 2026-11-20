package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Posts Wave Check lead records to the web endpoint, mirroring
 * saveWaveAuditLead in the web app (otdaisurfer/AI-Surfer-,
 * src/features/wave-audit/leadCapture.ts): 2 attempts, expects an
 * HTTP 200 with a JSON body whose status is "saved".
 *
 * NOTE: server sync from an Android client is implemented but
 * unverified — the endpoint is a web Pages Function and its behaviour
 * from this client has not been confirmed. Leads are always saved to
 * Room first, so a failed sync never loses a Wave Check.
 */
class WaveCheckSyncClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun submitLead(payloadJson: String): Boolean = withContext(Dispatchers.IO) {
        repeat(2) {
            val confirmed = runCatching {
                val body = payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(WAVE_CHECK_SUBMIT_URL)
                    .header("Accept", "application/json")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching false
                    val text = response.body?.string().orEmpty()
                    runCatching { JSONObject(text).optString("status") }.getOrNull() == "saved"
                }
            }.getOrDefault(false)
            if (confirmed) return@withContext true
        }
        false
    }

    companion object {
        const val WAVE_CHECK_SUBMIT_URL = "https://otdaisurfer.surf/api/wave-check-submit"
    }
}
