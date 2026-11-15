package com.example.data.network

import android.util.Base64
import com.example.data.model.ForgeBuildStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Mt5CompileWorkerResult(
    val status: ForgeBuildStatus,
    val compilerName: String,
    val errorsSummary: String,
    val warningsSummary: String,
    val outputFileName: String,
    val compiledBinaryBytes: ByteArray?
)

class Mt5CompilerClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun compileMq5(
        workerUrl: String?,
        fileName: String,
        sourceCode: String
    ): Mt5CompileWorkerResult = withContext(Dispatchers.IO) {
        val cleanUrl = workerUrl?.trim()?.trimEnd('/') ?: ""
        if (cleanUrl.isBlank()) {
            return@withContext Mt5CompileWorkerResult(
                status = ForgeBuildStatus.COMPILER_UNAVAILABLE,
                compilerName = "MetaEditor Worker (Not Connected)",
                errorsSummary = "No external MetaEditor compiler worker endpoint configured. Android does not bundle a native Windows MetaEditor binary. Configure an HTTPS MT5 Compiler Worker in Compilation Centre or Indicator Settings to produce .ex5 binaries.",
                warningsSummary = "",
                outputFileName = "",
                compiledBinaryBytes = null
            )
        }

        val json = JSONObject().apply {
            put("fileName", fileName)
            put("sourceCode", sourceCode)
        }
        val req = Request.Builder()
            .url("$cleanUrl/compile")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Mt5CompileWorkerResult(
                        status = ForgeBuildStatus.COMPILER_UNAVAILABLE,
                        compilerName = "MetaEditor Worker ($cleanUrl)",
                        errorsSummary = "Compiler worker returned HTTP ${resp.code}: ${bodyStr.take(400)}",
                        warningsSummary = "",
                        outputFileName = "",
                        compiledBinaryBytes = null
                    )
                }
                val parsed = JSONObject(bodyStr)
                val success = parsed.optBoolean("success", false)
                val errors = parsed.optString("errors", "")
                val warnings = parsed.optString("warnings", "")
                val compilerVer = parsed.optString("compilerVersion", "MetaEditor Remote Worker")
                val outName = parsed.optString("outputFileName", fileName.replace(".mq5", ".ex5"))
                val ex5B64 = parsed.optString("ex5Base64", "")
                val binary = if (success && ex5B64.isNotBlank()) {
                    Base64.decode(ex5B64, Base64.DEFAULT)
                } else null

                val status = when {
                    !success -> ForgeBuildStatus.COMPILATION_FAILED
                    binary == null -> ForgeBuildStatus.COMPILATION_FAILED
                    warnings.isNotBlank() -> ForgeBuildStatus.COMPILED_WARNINGS
                    else -> ForgeBuildStatus.COMPILED_SUCCESS
                }
                Mt5CompileWorkerResult(
                    status = status,
                    compilerName = compilerVer,
                    errorsSummary = if (!success && errors.isBlank()) "Compilation failed without detailed worker log." else errors,
                    warningsSummary = warnings,
                    outputFileName = if (binary != null) outName else "",
                    compiledBinaryBytes = binary
                )
            }
        } catch (e: Exception) {
            Mt5CompileWorkerResult(
                status = ForgeBuildStatus.COMPILER_UNAVAILABLE,
                compilerName = "MetaEditor Worker ($cleanUrl)",
                errorsSummary = "Failed reaching compiler worker: ${e.localizedMessage ?: "Network unreachable"}",
                warningsSummary = "",
                outputFileName = "",
                compiledBinaryBytes = null
            )
        }
    }
}
