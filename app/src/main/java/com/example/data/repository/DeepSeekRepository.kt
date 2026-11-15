package com.example.data.repository

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class DeepSeekCompletionResult(
    val text: String,
    val modelId: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val latencyMs: Long,
    val estimatedCostUsd: Double
)

data class DeepSeekModelDiscoveryResult(
    val connected: Boolean,
    val models: List<String>,
    val latencyMs: Long,
    val message: String,
    val rateLimited: Boolean = false
)

class DeepSeekRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun apiKey(): String = BuildConfig.DEEPSEEK_API_KEY.trim()

    fun isConfigured(): Boolean {
        val key = apiKey()
        return key.isNotBlank() && key != "MY_DEEPSEEK_API_KEY"
    }

    suspend fun discoverModels(): DeepSeekModelDiscoveryResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext DeepSeekModelDiscoveryResult(
                connected = false,
                models = emptyList(),
                latencyMs = 0L,
                message = "DEEPSEEK_API_KEY not configured. Add DEEPSEEK_API_KEY in the AI Studio Secrets panel."
            )
        }
        val started = System.currentTimeMillis()
        val req = Request.Builder()
            .url("https://api.deepseek.com/models")
            .header("Authorization", "Bearer ${apiKey()}")
            .get()
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - started
                val body = resp.body?.string().orEmpty()
                if (resp.code == 429) {
                    return@withContext DeepSeekModelDiscoveryResult(
                        connected = false,
                        models = emptyList(),
                        latencyMs = latency,
                        message = "DeepSeek rate limit (HTTP 429) reached.",
                        rateLimited = true
                    )
                }
                if (!resp.isSuccessful) {
                    return@withContext DeepSeekModelDiscoveryResult(
                        connected = false,
                        models = emptyList(),
                        latencyMs = latency,
                        message = "DeepSeek HTTP ${resp.code}: ${body.take(240)}"
                    )
                }
                val arr = JSONObject(body).optJSONArray("data") ?: JSONArray()
                val ids = (0 until arr.length()).mapNotNull { idx ->
                    arr.optJSONObject(idx)?.optString("id")?.takeIf { it.isNotBlank() }
                }
                DeepSeekModelDiscoveryResult(
                    connected = ids.isNotEmpty(),
                    models = ids,
                    latencyMs = latency,
                    message = if (ids.isNotEmpty()) "Connected (${ids.size} models discovered in ${latency}ms)" else "Connected, no models listed."
                )
            }
        } catch (e: Exception) {
            DeepSeekModelDiscoveryResult(
                connected = false,
                models = emptyList(),
                latencyMs = System.currentTimeMillis() - started,
                message = "DeepSeek request failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    suspend fun generateCompletion(
        modelId: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.2
    ): DeepSeekCompletionResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            throw IllegalStateException("DEEPSEEK_API_KEY is missing. Set it in the AI Studio Secrets panel.")
        }
        val started = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("model", modelId.ifBlank { "deepseek-chat" })
            put("temperature", temperature)
            put("stream", false)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }
            )
        }
        val req = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .header("Authorization", "Bearer ${apiKey()}")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val latency = System.currentTimeMillis() - started
            val raw = resp.body?.string().orEmpty()
            if (resp.code == 429) {
                throw IllegalStateException("DeepSeek rate limit exceeded (HTTP 429).")
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("DeepSeek API error HTTP ${resp.code}: ${raw.take(260)}")
            }
            val root = JSONObject(raw)
            val content = root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (content.isBlank()) {
                throw IllegalStateException("DeepSeek returned an empty completion.")
            }
            val usage = root.optJSONObject("usage")
            val promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0
            val compTokens = usage?.optInt("completion_tokens", 0) ?: 0
            val totalTokens = usage?.optInt("total_tokens", promptTokens + compTokens) ?: (promptTokens + compTokens)
            val estCost = (promptTokens * 0.27 + compTokens * 1.10) / 1_000_000.0
            DeepSeekCompletionResult(
                text = content,
                modelId = root.optString("model", modelId),
                promptTokens = promptTokens,
                completionTokens = compTokens,
                totalTokens = totalTokens,
                latencyMs = latency,
                estimatedCostUsd = estCost
            )
        }
    }

    fun streamCompletion(
        modelId: String,
        systemPrompt: String,
        userPrompt: String
    ): Flow<String> = flow {
        if (!isConfigured()) {
            throw IllegalStateException("DEEPSEEK_API_KEY is missing. Set it in the AI Studio Secrets panel.")
        }
        val payload = JSONObject().apply {
            put("model", modelId.ifBlank { "deepseek-chat" })
            put("temperature", 0.2)
            put("stream", true)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }
            )
        }
        val req = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .header("Authorization", "Bearer ${apiKey()}")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val sb = StringBuilder()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val errBody = resp.body?.string().orEmpty()
                throw IllegalStateException("DeepSeek stream error HTTP ${resp.code}: ${errBody.take(240)}")
            }
            val body = resp.body ?: throw IllegalStateException("Empty response stream from DeepSeek.")
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim().orEmpty()
                    if (!trimmed.startsWith("data:")) continue
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val obj = try { JSONObject(data) } catch (_: Exception) { null } ?: continue
                    val delta = obj.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                        .orEmpty()
                    if (delta.isNotEmpty()) {
                        sb.append(delta)
                        emit(sb.toString())
                    }
                }
            }
        }
        if (sb.isBlank()) {
            throw IllegalStateException("DeepSeek stream completed with no text.")
        }
    }.flowOn(Dispatchers.IO)
}
