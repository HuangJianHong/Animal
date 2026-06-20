package com.example.animal.animal.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animal.animal.entity.Animal
import com.example.animal.animal.viewmodel.AnimalMainViewModel
import com.example.animal.chat.view.ChatActivity
import com.example.animal.databinding.ActivityAnimalMainBinding

/**
 * 首页：竖向 RecyclerView 展示本地静态 10 只小动物。
 *
 * MVVM 约束：本 Activity 仅负责 UI 展示与点击事件分发，
 * 数据由 [AnimalMainViewModel] 提供（本地写死，无网络请求）。
 */
class AnimalMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimalMainBinding
    private val viewModel: AnimalMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimalMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvAnimals.layoutManager = LinearLayoutManager(this)

        // 观察动物列表，刷新 RecyclerView
        viewModel.animalList.observe(this) { list ->
            binding.rvAnimals.adapter = AnimalAdapter(list) { animal ->
                openChat(animal)
            }
        }
    }

    /**
     * 点击 Item：携带完整 Animal 实体跳转聊天页。
     */
    private fun openChat(animal: Animal) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(Animal.EXTRA_KEY, animal)
        }
        startActivity(intent)
    }
}
