package com.bottazzini.trasloco.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.R

class DeckGridAdapter(
    private val decks: List<CardDeck>,
    private val onDeckSelected: (CardDeck) -> Unit,
) : RecyclerView.Adapter<DeckGridAdapter.TileViewHolder>() {

    private var selectedId: String = ""

    fun setSelectedId(id: String) {
        val oldPos = decks.indexOfFirst { it.id == selectedId }
        val newPos = decks.indexOfFirst { it.id == id }
        selectedId = id
        if (oldPos >= 0) notifyItemChanged(oldPos)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView
        val prev1: ImageView = itemView.findViewById(R.id.tilePrev1)
        val prev2: ImageView = itemView.findViewById(R.id.tilePrev2)
        val prev3: ImageView = itemView.findViewById(R.id.tilePrev3)
    }

    override fun getItemCount() = decks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck_grid_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val deck = decks[position]
        val ctx = holder.root.context
        val isSelected = deck.id == selectedId

        val names = listOf("${deck.id}_b1", "${deck.id}_c1", "${deck.id}_d1")
        val previews = listOf(holder.prev1, holder.prev2, holder.prev3)
        names.zip(previews).forEach { (name, img) ->
            val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
            img.setImageDrawable(
                ContextCompat.getDrawable(ctx, if (resId != 0) resId else R.drawable.zero)
            )
            img.alpha = if (deck.available) 1f else 0.4f
        }

        holder.root.isSelected = isSelected
        holder.root.alpha = if (deck.available) 1f else 0.4f
        holder.root.isClickable = deck.available
        if (deck.available) {
            holder.root.setOnClickListener { onDeckSelected(deck) }
        } else {
            holder.root.setOnClickListener(null)
        }
    }
}
