package com.bottazzini.trasloco

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.utils.AchievementDef

class AchievementAdapter(
    private val context: Context,
    private val defs: List<AchievementDef>,
    private val unlockedMap: Map<String, Long>,
    private val onTap: (AchievementDef, isUnlocked: Boolean, unlockedAt: Long?) -> Unit
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.achievementItemIcon)
        val name: TextView = view.findViewById(R.id.achievementItemName)
        val lock: TextView = view.findViewById(R.id.achievementItemLock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val def = defs[position]
        val unlockedAt = unlockedMap[def.id]
        val isUnlocked = unlockedAt != null

        holder.icon.text = def.icon
        holder.name.text = context.getString(def.nameRes)
        holder.name.alpha = if (isUnlocked) 1f else 0.35f
        holder.icon.alpha = if (isUnlocked) 1f else 0.35f
        holder.lock.visibility = if (isUnlocked) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onTap(def, isUnlocked, unlockedAt)
        }
    }

    override fun getItemCount() = defs.size
}
