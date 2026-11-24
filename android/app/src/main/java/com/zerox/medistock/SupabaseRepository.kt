package com.zerox.medistock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SetupInput(
    val email: String, val password: String, val createAccount: Boolean,
    val ownerName: String, val ownerPhone: String,
    val shopName: String, val shopPhone: String, val shopAddress: String
)

data class SetupResult(val success: Boolean, val message: String)

class SupabaseRepository {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun createAccountAndShop(input: SetupInput): SetupResult = withContext(Dispatchers.IO) {
        try {
            val authPath = if (input.createAccount) "/auth/v1/signup" else "/auth/v1/token?grant_type=password"
            val authBody = JSONObject().put("email", input.email).put("password", input.password)
            val auth = request(authPath, "POST", authBody, null)
            val accessToken = auth.optString("access_token")
            val user = auth.optJSONObject("user") ?: auth.optJSONObject("session")?.optJSONObject("user")
            val userId = user?.optString("id").orEmpty()
            if (accessToken.isBlank() || userId.isBlank()) {
                return@withContext SetupResult(false, if (input.createAccount)
                    "Account request submitted. Check your email to verify it, then sign in."
                else "Sign-in did not return a session. Check your email and password.")
            }

            val profile = JSONObject()
                .put("id", userId)
                .put("full_name", input.ownerName)
                .put("phone", input.ownerPhone)
            request("/rest/v1/owner_profiles", "POST", profile, accessToken, "resolution=merge-duplicates")

            val shop = JSONObject()
                .put("owner_id", userId)
                .put("name", input.shopName)
                .put("phone", input.shopPhone)
                .put("address", input.shopAddress)
            request("/rest/v1/shops", "POST", shop, accessToken)
            SetupResult(true, "Account and shop created successfully.")
        } catch (e: Exception) {
            SetupResult(false, e.message ?: "Could not connect to Supabase.")
        }
    }

    private fun request(path: String, method: String, body: JSONObject, token: String?, prefer: String? = null): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("apikey", apiKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            doOutput = true
        }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            val detail = try { JSONObject(response).optString("message").ifBlank { response } } catch (_: Exception) { response }
            throw IllegalStateException("Supabase error ($code): $detail")
        }
        return if (response.isBlank()) JSONObject() else JSONObject(response)
    }
}
