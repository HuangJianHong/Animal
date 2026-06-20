package com.example.animal.demo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animal.demo.model.UserInfo
import com.example.animal.net.core.ILoadingView
import com.example.animal.net.core.launchHttp
import kotlinx.coroutines.launch

/**
 * 示例 ViewModel：演示在 viewModelScope 中安全发起网络请求。
 *
 * 两种写法都给出：
 * 1. 回调式 [launchHttp]：自动线程切换 + Loading + 防重复 + 生命周期取消；
 * 2. 协程式：直接 viewModelScope.launch + try/catch（见 [loginByCoroutine]）。
 */
class DemoViewModel : ViewModel() {

    private val repository = DemoRepository()

    /** 用户信息（成功） */
    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    /** 错误提示 */
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * 写法一：回调式，最省心。页面销毁时随 viewModelScope 自动取消。
     */
    fun loadUserInfo(userId: String, loading: ILoadingView? = null) {
        viewModelScope.launchHttp(
            requestKey = "getUserInfo_$userId", // 防重复
            loading = loading,
            loadingMsg = "正在加载用户信息...",
            onSuccess = { user -> _userInfo.value = user },
            onError = { e -> _error.value = e.errorMsg }
        ) {
            // 直接调用 Api，返回 ResponseBean，框架自动解包
            com.example.animal.net.core.HttpManager
                .api(com.example.animal.net.api.DemoApiService::class.java)
                .getUserInfo(userId)
        }
    }

    /**
     * 写法二：纯协程式，通过 Repository 拿到纯 data。
     */
    fun loginByCoroutine(username: String, password: String) {
        viewModelScope.launch {
            try {
                val result = repository.login(username, password)
                // 登录成功，保存 token（后续请求自动携带）
                com.example.animal.net.config.NetConfig.token = result.token
            } catch (e: com.example.animal.net.exception.ApiException) {
                _error.value = e.errorMsg
            }
        }
    }
}
