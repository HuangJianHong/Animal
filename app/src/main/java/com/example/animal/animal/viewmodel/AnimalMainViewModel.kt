package com.example.animal.animal.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.animal.animal.entity.Animal
import com.example.animal.animal.entity.AnimalProfiles

/**
 * 首页 ViewModel。
 *
 * 职责：加载本地静态写死的 10 只小动物列表（无任何网络请求），
 * 通过 LiveData 对外暴露动物列表，供 Activity 观察刷新 UI。
 */
class AnimalMainViewModel : ViewModel() {

    /** 动物列表 LiveData */
    private val _animalList = MutableLiveData<List<Animal>>()
    val animalList: LiveData<List<Animal>> = _animalList

    init {
        // 直接加载本地静态数据
        loadLocalAnimals()
    }

    /** 加载本地写死的小动物数据 */
    private fun loadLocalAnimals() {
        _animalList.value = AnimalProfiles.all()
    }
}
