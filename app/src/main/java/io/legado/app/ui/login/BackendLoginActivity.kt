package io.legado.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityBackendLoginBinding
import io.legado.app.help.backend.BackendApi
import io.legado.app.help.backend.BackendAuth
import io.legado.app.ui.main.MainActivity
import kotlinx.coroutines.*

class BackendLoginActivity : VMBaseActivity<ActivityBackendLoginBinding, BackendLoginViewModel>() {

    private var isLoginMode = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 如果已配置服务器且已登录，直接验证
        if (BackendAuth.serverUrl.isNotEmpty() && BackendAuth.isLoggedIn()) {
            verifyAndProceed()
            return
        }

        initViews()
    }

    private fun initViews() {
        // 如果已配置服务器，隐藏服务器地址输入
        if (BackendAuth.serverUrl.isNotEmpty()) {
            binding.etServerUrl.setText(BackendAuth.serverUrl)
            binding.etServerUrl.isEnabled = false
            binding.etServerUrl.alpha = 0.5f
        }

        binding.btnAction.setOnClickListener {
            val serverUrl = binding.etServerUrl.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (serverUrl.isEmpty()) {
                showToast("请输入后台服务器地址")
                return@setOnClickListener
            }
            
            if (username.isEmpty() || password.isEmpty()) {
                showToast("请输入用户名和密码")
                return@setOnClickListener
            }

            // 设置服务器地址
            if (BackendAuth.serverUrl.isEmpty()) {
                BackendAuth.setServerUrl(serverUrl)
            }

            binding.btnAction.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.IO) {
                val deviceId = BackendAuth.getDeviceId(this@BackendLoginActivity)
                val deviceName = BackendAuth.getDeviceName()

                val result = if (isLoginMode) {
                    BackendApi.login(username, password, deviceId)
                } else {
                    BackendApi.register(username, password, deviceId)
                }

                withContext(Dispatchers.Main) {
                    binding.btnAction.isEnabled = true
                    binding.progressBar.visibility = View.GONE

                    if (result != null) {
                        BackendAuth.saveLogin(result.first, result.second)
                        BackendApi.registerDevice(deviceId, deviceName)
                        proceedToMain()
                    } else {
                        showToast(if (isLoginMode) "登录失败，请检查用户名和密码" else "注册失败，用户名可能已存在")
                    }
                }
            }
        }

        binding.tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        updateUI()
    }

    private fun updateUI() {
        if (isLoginMode) {
            binding.btnAction.text = "登录"
            binding.tvSwitchMode.text = "没有账号？去注册"
        } else {
            binding.btnAction.text = "注册"
            binding.tvSwitchMode.text = "已有账号？去登录"
        }
    }

    private fun verifyAndProceed() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = BackendApi.verify()
            withContext(Dispatchers.Main) {
                if (result != null) {
                    result.user?.let { BackendAuth.updateUser(it) }
                    proceedToMain()
                } else {
                    BackendAuth.logout()
                    // 显示登录界面
                    initViews()
                }
            }
        }
    }

    private fun proceedToMain() {
        setResult(RESULT_OK)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override val binding by lazy { ActivityBackendLoginBinding.inflate(layoutInflater) }
    override val viewModel by lazy { getViewModel(BackendLoginViewModel::class.java) }
}
