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
            val displayName = BackendAuth.getDisplayName()
            binding.tvDisplayName.text = displayName
            binding.tvUserGroup.text = BackendAuth.getGroupDescription()
            binding.tvAvatar.text = displayName.firstOrNull()?.uppercase() ?: "U"
            binding.tvGroupBadge.visibility = View.VISIBLE
            binding.tvGroupBadge.text = BackendAuth.groupName.ifEmpty { BackendAuth.getGroupDescription() }
            binding.tvServerInfo.text = "服务器：$serverUrl"
        } else {
            binding.tvDisplayName.text = "未登录"
            binding.tvUserGroup.text = "请先登录后台"
            binding.tvAvatar.text = "?"
            binding.tvGroupBadge.visibility = View.GONE
            binding.tvServerInfo.text = "服务器：未配置"
        }

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

        binding.btnLogout.setOnClickListener {
            if (isLoggedIn) {
                requireContext().alert("退出登录", "确定退出当前账号？") {
                    positiveButton("确定") {
                        BackendAuth.logout()
                        toastOnUi("已退出登录")
                        setupUserInfoCard()
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

    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            putPrefBoolean(PreferKey.webService, WebService.isRun)
            addPreferencesFromResource(R.xml.pref_main)
            findPreference<SwitchPreference>("webService")?.onLongClick {
                if (!WebService.isRun) return@onLongClick false
                context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, i ->
                    when (i) {
                        0 -> context?.sendToClip(it.summary.toString())
                        1 -> context?.openUrl(it.summary.toString())
                    }
                }
                true
            }
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                findPreference<SwitchPreference>(PreferKey.webService)?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) WebService.hostAddress else getString(R.string.web_service_desc)
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) WebService.start(requireContext())
                    else WebService.stop(requireContext())
                }
                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "dictRuleManage" -> startActivity<DictRuleActivity>()
                "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> { putExtra("configTag", ConfigTag.OTHER_CONFIG) }
                "web_dav_setting" -> startActivity<ConfigActivity> { putExtra("configTag", ConfigTag.BACKUP_CONFIG) }
                "theme_setting" -> startActivity<ConfigActivity> { putExtra("configTag", ConfigTag.THEME_CONFIG) }
                "fileManage" -> startActivity<FileManageActivity>()
                "readRecord" -> startActivity<ReadRecordActivity>()
                "about" -> startActivity<AboutActivity>()
                "exit" -> activity?.finish()
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}
