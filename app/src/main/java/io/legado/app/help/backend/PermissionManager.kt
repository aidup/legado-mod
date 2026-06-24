package io.legado.app.help.backend

/**
 * 后台权限管理器
 * 根据用户组控制功能开关
 */
object PermissionManager {

    /** 能否搜索 */
    fun canSearch(): Boolean {
        return BackendAuth.hasPermission("search")
    }

    /** 能否导入书源 */
    fun canImportSource(): Boolean {
        return BackendAuth.hasPermission("import_source")
    }

    /** 能否导出 */
    fun canExport(): Boolean {
        return BackendAuth.hasPermission("export")
    }

    /** 能否使用 TTS */
    fun canUseTTS(): Boolean {
        return BackendAuth.hasPermission("tts")
    }

    /** 能否自定义主题 */
    fun canCustomTheme(): Boolean {
        return BackendAuth.hasPermission("theme")
    }

    /** 能否写书架（添加/删除/分组） */
    fun canWriteBookshelf(): Boolean {
        return BackendAuth.hasPermission("bookshelf_write")
    }

    /** 能否看高级功能 */
    fun canUseAdvanced(): Boolean {
        return BackendAuth.hasPermission("advanced")
    }

    /** 是否为 VIP */
    fun isVip(): Boolean {
        return BackendAuth.groupId >= 3
    }

    /** 是否为管理员 */
    fun isAdmin(): Boolean {
        return BackendAuth.groupId == 99
    }
}
