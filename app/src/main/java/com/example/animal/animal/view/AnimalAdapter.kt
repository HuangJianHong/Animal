package com.example.animal.animal.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animal.animal.entity.Animal
import com.example.animal.databinding.ItemAnimalBinding
import com.example.animal.image.core.ImageLoader

/**
 * 首页小动物列表适配器。
 *
 * 复用项目已有 Glide 工具类 [ImageLoader] 加载头像（圆形 + 全局默认占位/失败图）。
 *
 * @param data    动物列表
 * @param onClick Item 点击回调
 */
class AnimalAdapter(
    private val data: List<Animal>,
    private val onClick: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdapter.AnimalHolder>() {

    inner class AnimalHolder(val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalHolder {
        val binding = ItemAnimalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AnimalHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimalHolder, position: Int) {
        val animal = data[position]
        holder.binding.tvName.text = animal.name
        // 复用全局 Glide 工具，圆形头像 + 全局默认占位/失败图
        ImageLoader.loadCircle(holder.binding.ivAvatar, animal.avatarResId)
        holder.binding.root.setOnClickListener { onClick(animal) }
    }

    override fun getItemCount(): Int = data.size
}
