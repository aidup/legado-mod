package io.legado.app.help.backend

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import org.json.JSONObject

/**
 * 后台鉴权管理器
 * 管理 token、用户信息、权限
 */
object BackendAuth {

    private const val PREFS_NAME = "backend_auth"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_GROUP_ID = "group_id"
    private const val KEY_GROUP_NAME = "group_name"
    private const val KEY_PERMISSIONS = "permissions"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_REMEMBER_USER = "remember_username"
    private const val KEY_REMEMBER_PASS = "remember_password"

    private var prefs: SharedPreferences? = null

    var token: String? = null
        private set
    
    var userId: Int = 0
        private set
    
    var username: String = ""
        private set
    
    var nickname: String = ""
        private set
    
    var avatar: String = ""
        private set
    
    var groupId: Int = 0
        private set
    
    var groupName: String = ""
        private set
    
    var permissions: List<String> = emptyList()
        private set
    
    var serverUrl: String = ""
        private set

    /**
     * 初始化（App 启动时调用）
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        token = prefs?.getString(KEY_TOKEN, null)
        userId = prefs?.getInt(KEY_USER_ID, 0) ?: 0
        username = prefs?.getString(KEY_USERNAME, "") ?: ""
        nickname = prefs?.getString(KEY_NICKNAME, "") ?: ""
        avatar = prefs?.getString(KEY_AVATAR, "") ?: ""
        groupId = prefs?.getInt(KEY_GROUP_ID, 0) ?: 0
        groupName = prefs?.getString(KEY_GROUP_NAME, "") ?: ""
        permissions = prefs?.getString(KEY_PERMISSIONS, "")?.split(",") ?: emptyList()
        serverUrl = prefs?.getString(KEY_SERVER_URL, "") ?: ""
        
        // 设置 API 基础 URL
        if (serverUrl.isNotEmpty()) {
            BackendApi.BASE_URL = serverUrl
        }
    }

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean = token != null && token!!.isNotEmpty()

    /**
     * 是否有指定权限
     */
    fun hasPermission(perm: String): Boolean {
        return permissions.contains("*") || permissions.contains(perm)
    }

    /**
     * 验证登录状态
     * @return AuthResult
     */
    fun verify(context: Context): AuthResult {
        // 检查是否配置了服务器地址
        if (serverUrl.isEmpty()) {
            return AuthResult.NeedConfig
        }
        
        // 检查是否已登录
        if (!isLoggedIn()) {
            return AuthResult.NeedLogin
        }

        // 验证 token
        return try {
            val result = BackendApi.verify()
            if (result != null) {
                result.user?.let { updateUser(it) }
                AuthResult.Authenticated(
                    user = result.user ?: JSONObject(),
                    notice = result.notice,
                    latestVersion = result.latestVersion
                )
            } else {
                // token 无效，清除登录状态
                logout()
                AuthResult.NeedLogin
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "未知错误")
        }
    }

    /**
     * 保存登录信息
     */
    fun saveLogin(token: String, user: JSONObject) {
        this.token = token
        this.userId = user.optInt("id")
        this.username = user.optString("username")
        this.nickname = user.optString("nickname")
        this.avatar = user.optString("avatar")
        this.groupId = user.optInt("group_id")
        this.groupName = user.optString("group_name")
        
        val perms = user.optString("permissions", "[]")
        try {
            val arr = org.json.JSONArray(perms)
            this.permissions = (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            this.permissions = emptyList()
        }

        prefs?.edit()?.apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_NICKNAME, nickname)
            putString(KEY_AVATAR, avatar)
            putInt(KEY_GROUP_ID, groupId)
            putString(KEY_GROUP_NAME, groupName)
            putString(KEY_PERMISSIONS, permissions.joinToString(","))
            apply()
        }
    }

    /**
     * 设置服务器地址
     */
    fun setServerUrl(url: String) {
        serverUrl = url.trimEnd('/')
        BackendApi.BASE_URL = serverUrl
        prefs?.edit()?.putString(KEY_SERVER_URL, serverUrl)?.apply()
    }

    /**
     * 更新用户信息
     */
    fun updateUser(user: JSONObject) {
        this.nickname = user.optString("nickname", nickname)
        this.avatar = user.optString("avatar", avatar)
        this.groupId = user.optInt("group_id", groupId)
        this.groupName = user.optString("group_name", groupName)
        
        prefs?.edit()?.apply {
            putString(KEY_NICKNAME, nickname)
            putString(KEY_AVATAR, avatar)
            putInt(KEY_GROUP_ID, groupId)
            putString(KEY_GROUP_NAME, groupName)
            apply()
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        token = null
        userId = 0
        username = ""
        nickname = ""
        avatar = ""
        groupId = 0
        groupName = ""
        permissions = emptyList()
        
        prefs?.edit()?.apply {
            remove(KEY_TOKEN)
            apply()
        }
    }

    /**
     * 获取设备 ID
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    /**
     * 获取设备名称
     */
    fun getDeviceName(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    }

    /**
     * 保存记住的账号密码
     */
    fun saveRememberedLogin(username: String, password: String) {
        prefs?.edit()?.apply {
            putString(KEY_REMEMBER_USER, username)
            putString(KEY_REMEMBER_PASS, password)
            apply()
        }
    }

    /**
     * 获取记住的账号密码
     * @return Pair(username, password) 或 null
     */
    fun getRememberedLogin(): Pair<String, String>? {
        val u = prefs?.getString(KEY_REMEMBER_USER, null)
        val p = prefs?.getString(KEY_REMEMBER_PASS, null)
        return if (!u.isNullOrEmpty() && !p.isNullOrEmpty()) Pair(u, p) else null
    }

    /**
     * 清除记住的账号密码
     */
    fun clearRememberedLogin() {
        prefs?.edit()?.apply {
            remove(KEY_REMEMBER_USER)
            remove(KEY_REMEMBER_PASS)
            apply()
        }
    }

    /**
     * 获取用户显示名称（优先昵称，其次用户名）
     */
    fun getDisplayName(): String {
        return nickname.ifEmpty { username }
    }

    /**
     * 获取用户组描述
     */
    fun getGroupDescription(): String {
        return when {
            groupId == 99 -> "管理员"
            groupId >= 3 -> "VIP 用户"
            groupId >= 2 -> "普通用户"
            else -> "试用用户"
        }
    }
}
