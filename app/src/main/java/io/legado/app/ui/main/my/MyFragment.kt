package io.legado.app.ui.main.my

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentMyConfigBinding
import io.legado.app.help.backend.BackendAuth
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.prefs.NameListPreference
import io.legado.app.lib.prefs.SwitchPreference
import io.legado.app.lib.prefs.fragment.PreferenceFragment
import io.legado.app.lib.theme.primaryColor
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.ReadRecordActivity
import io.legado.app.ui.book.bookmark.AllBookmarkActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.book.toc.rule.TxtTocRuleActivity
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigTag
import io.legado.app.ui.dict.rule.DictRuleActivity
import io.legado.app.ui.file.FileManageActivity
import io.legado.app.ui.login.BackendLoginActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.openUrl
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showHelp
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        setupUserInfoCard()

        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    private fun setupUserInfoCard() {
        val isLoggedIn = BackendAuth.isLoggedIn()
        val serverUrl = BackendAuth.serverUrl

        if (isLoggedIn) {
            // 显示用户信息
            val displayName = BackendAuth.getDisplayName()
            binding.tvDisplayName.text = displayName
            binding.tvUserGroup.text = BackendAuth.getGroupDescription()

            // 头像首字母
            val initial = displayName.firstOrNull()?.uppercase() ?: "U"
            binding.tvAvatar.text = initial

            // 用户组标签
            binding.tvGroupBadge.visibility = View.VISIBLE
            binding.tvGroupBadge.text = BackendAuth.groupName.ifEmpty { BackendAuth.getGroupDescription() }

            // 服务器信息
            binding.tvServerInfo.text = "服务器：$serverUrl"
        } else {
            binding.tvDisplayName.text = "未登录"
            binding.tvUserGroup.text = "请先登录后台"
            binding.tvAvatar.text = "?"
            binding.tvGroupBadge.visibility = View.GONE
            binding.tvServerInfo.text = "服务器：未配置"
        }

        // 切换账号
        binding.btnSwitchAccount.setOnClickListener {
            if (isLoggedIn) {
                requireContext().alert("切换账号", "当前账号将被退出，确定切换？") {
                    positiveButton("确定") {
                        BackendAuth.logout()
                        startActivity(Intent(requireContext(), BackendLoginActivity::class.java))
                    }
                    negativeButton("取消") {}
                }
            } else {
                startActivity(Intent(requireContext(), BackendLoginActivity::class.java))
            }
        }

        // 退出登录
        binding.btnLogout.setOnClickListener {
            if (isLoggedIn) {
                requireContext().alert("退出登录", "确定退出当前账号？") {
                    positiveButton("确定") {
                        BackendAuth.logout()
                        toastOnUi("已退出登录")
                        // 刷新页面
                        setupUserInfoCard()
                        // 跳转登录页
                        startActivity(Intent(requireContext(), BackendLoginActivity::class.java))
                    }
                    negativeButton("取消") {}
                }
            } else {
                startActivity(Intent(requireContext(), BackendLoginActivity::class.java))
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> showHelp("appHelp")
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.main_my_config)
            findPreference<Preference>("web_dav_sync")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<ConfigActivity> {
                        putExtra("configTag", ConfigTag.BACKUP_CONFIG.name)
                    }
                    true
                }
            }
            findPreference<Preference>("web_dav_restore")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<ConfigActivity> {
                        putExtra("configTag", ConfigTag.BACKUP_CONFIG.name)
                    }
                    true
                }
            }
            findPreference<Preference>("replace")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<ReplaceRuleActivity>()
                    true
                }
            }
            findPreference<Preference>("bookmark")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<AllBookmarkActivity>()
                    true
                }
            }
            findPreference<Preference>("book_source")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<BookSourceActivity>()
                    true
                }
            }
            findPreference<Preference>("txt_toc_rule")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<TxtTocRuleActivity>()
                    true
                }
            }
            findPreference<Preference>("dict_rule")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<DictRuleActivity>()
                    true
                }
            }
            findPreference<Preference>("file_manage")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<FileManageActivity>()
                    true
                }
            }
            findPreference<Preference>("read_record")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<ReadRecordActivity>()
                    true
                }
            }
            findPreference<Preference>("about")?.let {
                it.setOnPreferenceClickListener {
                    startActivity<AboutActivity>()
                    true
                }
            }
            findPreference<Preference>("web_service")?.let {
                it.setOnPreferenceClickListener {
                    requireContext().selector(items = listOf("打开", "关闭")) { _, index ->
                        when (index) {
                            0 -> WebService.start(requireContext())
                            1 -> WebService.stop(requireContext())
                        }
                    }
                    true
                }
            }
            findPreference<Preference>("theme")?.let {
                it.setOnPreferenceClickListener {
                    ThemeConfig.applyTheme(requireActivity())
                    true
                }
            }
            findPreference<Preference>("log")?.let {
                it.setOnPreferenceClickListener {
                    LogUtils.show(requireContext())
                    true
                }
            }
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                PreferKey.themeMode -> ThemeConfig.applyTheme(requireActivity())
                PreferKey.showDiscovery -> {
                    postEvent(EventBus.NOTIFY_MAIN, true)
                }
                PreferKey.showRSS -> {
                    postEvent(EventBus.NOTIFY_MAIN, true)
                }
            }
        }
    }
}
