package io.legado.app.help.backend

import android.content.Context

/**
 * 后台权限管理器
 * 根据用户组控制功能开关
 */
object PermissionManager {

    /** 能否搜索 */
    fun canSearch(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "search")
    }

    /** 能否导入书源 */
    fun canImportSource(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "import_source")
    }

    /** 能否导出 */
    fun canExport(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "export")
    }

    /** 能否使用 TTS */
    fun canUseTTS(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "tts")
    }

    /** 能否自定义主题 */
    fun canCustomTheme(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "theme")
    }

    /** 能否写书架（添加/删除/分组） */
    fun canWriteBookshelf(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "bookshelf_write")
    }

    /** 能否看高级功能 */
    fun canUseAdvanced(context: Context): Boolean {
        return BackendAuth.hasPermission(context, "advanced")
    }

    /** 是否为 VIP */
    fun isVip(context: Context): Boolean {
        return BackendAuth.getGroupId(context) >= 3
    }

    /** 是否为管理员 */
    fun isAdmin(context: Context): Boolean {
        return BackendAuth.getGroupId(context) == 99
    }

    /** 检查是否过期 */
    fun isExpired(context: Context): Boolean {
        val expireAt = BackendAuth.getExpireAt(context) ?: return false
        return try {
            val expire = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .parse(expireAt.replace("Z", "+00:00").substring(0, 19))
            expire != null && expire.before(java.util.Date())
        } catch (e: Exception) {
            false
        }
    }
}
