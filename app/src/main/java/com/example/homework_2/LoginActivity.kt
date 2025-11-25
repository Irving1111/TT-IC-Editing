package com.example.homework_2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homework_2.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: android.content.SharedPreferences

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 检查是否已登录
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            navigateToProfile()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 登录按钮
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // 忘记密码
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "忘记密码功能", Toast.LENGTH_SHORT).show()
        }

        // 微信登录
        binding.btnWechatLogin.setOnClickListener {
            Toast.makeText(this, "微信登录", Toast.LENGTH_SHORT).show()
        }

        // Apple登录
        binding.btnAppleLogin.setOnClickListener {
            Toast.makeText(this, "Apple登录", Toast.LENGTH_SHORT).show()
        }

        // 立即注册
        binding.tvRegister.setOnClickListener {
            Toast.makeText(this, "立即注册", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // 验证输入
        if (username.isEmpty()) {
            binding.tilUsername.error = "请输入账号"
            return
        } else {
            binding.tilUsername.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "请输入密码"
            return
        } else {
            binding.tilPassword.error = null
        }

        // 获取保存的账号密码
        val savedUsername = sharedPreferences.getString(KEY_USERNAME, null)
        val savedPassword = sharedPreferences.getString(KEY_PASSWORD, null)

        // 首次登录，保存账号密码
        if (savedUsername == null && savedPassword == null) {
            saveUserCredentials(username, password)
            Toast.makeText(this, "首次登录，账号已保存", Toast.LENGTH_SHORT).show()
            navigateToProfile()
        } else {
            // 验证账号密码
            if (username == savedUsername && password == savedPassword) {
                // 登录成功
                sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                navigateToProfile()
            } else {
                // 登录失败
                Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserCredentials(username: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }
}
