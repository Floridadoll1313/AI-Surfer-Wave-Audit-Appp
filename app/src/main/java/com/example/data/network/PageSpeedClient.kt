package com.example.data.network

import com.example.data.model.LhrAuditReport
import com.example.data.parser.LhrJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PageSpeedClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun runAudit(targetUrl: String, strategy: String = "mobile"): Result<LhrAuditReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = targetUrl.trim().let {
                    if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
                }
                val endpoint = "https://www.googleapis.com/pagespeedonline/v5/runPagespeed"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("url", normalized)
                    .addQueryParameter("strategy", strategy.lowercase())
                    .addQueryParameter("category", "PERFORMANCE")
                    .addQueryParameter("category", "ACCESSIBILITY")
                    .addQueryParameter("category", "BEST_PRACTICES")
                    .addQueryParameter("category", "SEO")
                    .build()

                val request = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val apiMessage = runCatching {
                            JSONObject(body).optJSONObject("error")?.optString("message")
                        }.getOrNull()
                        throw IllegalStateException(
                            apiMessage?.takeIf { it.isNotBlank() }
                                ?: "PageSpeed API error (HTTP ${response.code}). You can also import a local LHR .json file."
                        )
                    }
                    LhrJsonParser.parseLhrJson(
                        rawJson = body,
                        sourceLabel = "Live PageSpeed (${strategy.replaceFirstChar { it.uppercase() }})",
                    )
                }
            }
        }
}
