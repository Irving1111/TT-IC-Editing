package com.example.homework_2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homework_2.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sharedPreferences: android.content.SharedPreferences

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USERNAME = "username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 加载用户信息
        loadUserInfo()

        // 设置点击事件
        setupClickListeners()
    }

    private fun loadUserInfo() {
        // 从 SharedPreferences 获取用户名
        val username = sharedPreferences.getString(KEY_USERNAME, "用户名")
        binding.tvUsername.text = username
    }

    private fun setupClickListeners() {
        // 个人信息
        binding.llPersonalInfo.setOnClickListener {
            Toast.makeText(this, "个人信息", Toast.LENGTH_SHORT).show()
        }

        // 我的收藏
        binding.llMyFavorites.setOnClickListener {
            Toast.makeText(this, "我的收藏", Toast.LENGTH_SHORT).show()
        }

        // 课程历史
        binding.llCourseHistory.setOnClickListener {
            Toast.makeText(this, "课程历史", Toast.LENGTH_SHORT).show()
        }

        // 设置
        binding.llSettings.setOnClickListener {
            Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show()
        }

        // 关于我们
        binding.llAbout.setOnClickListener {
            Toast.makeText(this, "关于我们", Toast.LENGTH_SHORT).show()
        }

        // 意见反馈
        binding.llFeedback.setOnClickListener {
            Toast.makeText(this, "意见反馈", Toast.LENGTH_SHORT).show()
        }

        // 退出登录
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // 清除登录状态
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", false)
            apply()
        }

        // 跳转到登录页面
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
    }
}
