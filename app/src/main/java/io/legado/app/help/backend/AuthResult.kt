package io.legado.app.help.backend

import org.json.JSONObject

/**
 * 鉴权结果
 */
sealed class AuthResult {
    /**
     * 已认证
     * @param user 用户信息
     * @param notice 公告（可选）
     * @param latestVersion 最新版本（可选）
     */
    data class Authenticated(
        val user: JSONObject,
        val notice: JSONObject?,
        val latestVersion: JSONObject?
    ) : AuthResult()

    /**
     * 需要配置后台地址
     */
    object NeedConfig : AuthResult()

    /**
     * 需要登录
     */
    object NeedLogin : AuthResult()

    /**
     * 错误
     * @param message 错误信息
     */
    data class Error(val message: String) : AuthResult()
}
