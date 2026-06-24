package io.legado.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityBackendLoginBinding
import io.legado.app.help.backend.BackendApi
import io.legado.app.help.backend.BackendAuth
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.*

class BackendLoginActivity : VMBaseActivity<ActivityBackendLoginBinding, BackendLoginViewModel>() {

    override val binding by viewBinding(ActivityBackendLoginBinding::inflate)
    override val viewModel by viewModels<BackendLoginViewModel>()

    private var isLoginMode = true
    private var passwordVisible = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // 如果已配置服务器且已登录，直接验证
        if (BackendAuth.serverUrl.isNotEmpty() && BackendAuth.isLoggedIn()) {
            verifyAndProceed()
            return
        }
        initViews()
    }

    private fun initViews() {
        // 恢复记住的账号
        val remembered = BackendAuth.getRememberedLogin()
        if (remembered != null) {
            binding.etUsername.setText(remembered.first)
            binding.etPassword.setText(remembered.second)
            binding.cbRemember.isChecked = true
        }

        // 如果已配置服务器，锁定地址输入
        if (BackendAuth.serverUrl.isNotEmpty()) {
            binding.etServerUrl.setText(BackendAuth.serverUrl)
            binding.etServerUrl.isEnabled = false
            binding.etServerUrl.alpha = 0.6f
            binding.tvServerStatus.text = "已连接"
            binding.tvServerStatus.setTextColor(resources.getColor(R.color.primary, null))
        }

        // 密码显示/隐藏
        binding.btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        // 登录/注册按钮
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
                        // 记住密码
                        if (binding.cbRemember.isChecked) {
                            BackendAuth.saveRememberedLogin(username, password)
                        } else {
                            BackendAuth.clearRememberedLogin()
                        }
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
}
