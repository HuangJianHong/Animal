package com.example.animal

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.animal.databinding.ActivityMainBinding
import com.example.animal.demo.DemoViewModel
import com.example.animal.demo.LoadingDialog

/**
 * 示例页面：演示在 Activity 中通过 ViewModel 发起网络请求。
 *
 * 关键点：
 * - 使用 viewModels() 获取 ViewModel，请求绑定 viewModelScope，页面销毁自动取消；
 * - 传入 LoadingDialog 自动显示/隐藏加载弹窗；
 * - 通过 LiveData 观察结果与错误。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: DemoViewModel by viewModels()
    private val loadingDialog by lazy { LoadingDialog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 观察结果
        viewModel.userInfo.observe(this) { user ->
            binding.tvResult.text = "请求成功：${user.nickname}"
        }
        viewModel.error.observe(this) { msg ->
            binding.tvResult.text = "请求失败：$msg"
        }

        // 点击发起示例请求（自动 Loading + 防重复 + 生命周期安全）
        binding.btnRequest.setOnClickListener {
            viewModel.loadUserInfo(userId = "10086", loading = loadingDialog)
        }
    }
}
