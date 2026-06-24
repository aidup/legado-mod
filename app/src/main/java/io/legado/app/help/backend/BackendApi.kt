package io.legado.app.help.backend

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 后台 API 客户端
 * 与 Legado 后台管控系统通信
 */
object BackendApi {

    private const val TAG = "BackendApi"

    // 后台服务器地址，从 BackendAuth 动态获取
    val BASE_URL: String
        get() = BackendAuth.serverUrl.ifEmpty { "http://103.231.58.19:8900" }
    
    /**
     * 用户登录
     * @return Pair(token, userInfo) 或 null
     */
    fun login(username: String, password: String, deviceId: String): Pair<String, JSONObject>? {
        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("device_id", deviceId)
        }
        val response = post("/api/auth/login", body, needAuth = false) ?: return null
        if (response.optInt("code") != 0) {
            Log.e(TAG, "Login failed: ${response.optString("message")}")
            return null
        }
        val data = response.optJSONObject("data") ?: return null
        val token = data.optString("token")
        val user = data.optJSONObject("user") ?: return null
        return Pair(token, user)
    }

    /**
     * 用户注册
     * @return Pair(token, userInfo) 或 null
     */
    fun register(username: String, password: String, deviceId: String): Pair<String, JSONObject>? {
        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("device_id", deviceId)
        }
        val response = post("/api/auth/register", body, needAuth = false) ?: return null
        if (response.optInt("code") != 0) {
            Log.e(TAG, "Register failed: ${response.optString("message")}")
            return null
        }
        val data = response.optJSONObject("data") ?: return null
        val token = data.optString("token")
        val user = data.optJSONObject("user") ?: return null
        return Pair(token, user)
    }

    /**
     * 验证 Token（App 启动时调用）
     * @return VerifyResult 或 null（token 无效/过期/封禁）
     */
    fun verify(): VerifyResult? {
        val response = post("/api/auth/verify", JSONObject()) ?: return null
        if (response.optInt("code") != 0) return null
        val data = response.optJSONObject("data") ?: return null
        if (!data.optBoolean("valid")) return null
        
        return VerifyResult(
            user = data.optJSONObject("user"),
            notice = data.optJSONObject("notice"),
            latestVersion = data.optJSONObject("latest_version")
        )
    }

    /**
     * 获取书源列表（按用户组过滤）
     */
    fun getSources(): JSONArray? {
        val response = get("/api/sources") ?: return null
        if (response.optInt("code") != 0) return null
        return response.optJSONArray("data")
    }

    /**
     * 获取最新公告
     */
    fun getLatestNotice(): JSONObject? {
        val response = get("/api/notice/latest") ?: return null
        if (response.optInt("code") != 0) return null
        return response.optJSONObject("data")
    }

    /**
     * 获取最新版本
     */
    fun getLatestVersion(): JSONObject? {
        val response = get("/api/app/version") ?: return null
        if (response.optInt("code") != 0) return null
        return response.optJSONObject("data")
    }

    /**
     * 上报阅读进度
     */
    fun syncProgress(bookUrl: String, bookName: String, bookAuthor: String, 
                     chapterIndex: Int, chapterTitle: String, readPosition: Int): Boolean {
        val body = JSONObject().apply {
            put("book_url", bookUrl)
            put("book_name", bookName)
            put("book_author", bookAuthor)
            put("chapter_index", chapterIndex)
            put("chapter_title", chapterTitle)
            put("read_position", readPosition)
        }
        val response = post("/api/progress/sync", body) ?: return false
        return response.optInt("code") == 0
    }

    /**
     * 设备注册/心跳
     */
    fun registerDevice(deviceId: String, deviceName: String): Boolean {
        val body = JSONObject().apply {
            put("device_id", deviceId)
            put("device_name", deviceName)
        }
        val response = post("/api/device/register", body) ?: return false
        return response.optInt("code") == 0
    }

    // ========== HTTP 工具方法 ==========

    private fun get(path: String): JSONObject? {
        return try {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer ${BackendAuth.token}")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            readResponse(conn)
        } catch (e: Exception) {
            Log.e(TAG, "GET $path failed", e)
            null
        }
    }

    private fun post(path: String, body: JSONObject, needAuth: Boolean = true): JSONObject? {
        return try {
            val url = URL("$BASE_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            if (needAuth) {
                conn.setRequestProperty("Authorization", "Bearer ${BackendAuth.token}")
            }
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
            
            readResponse(conn)
        } catch (e: Exception) {
            Log.e(TAG, "POST $path failed", e)
            null
        }
    }

    private fun readResponse(conn: HttpURLConnection): JSONObject? {
        val reader = BufferedReader(InputStreamReader(
            if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        ))
        val response = reader.use { it.readText() }
        return try {
            JSONObject(response)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid JSON: $response")
            null
        }
    }

    data class VerifyResult(
        val user: JSONObject?,
        val notice: JSONObject?,
        val latestVersion: JSONObject?
    )
}
