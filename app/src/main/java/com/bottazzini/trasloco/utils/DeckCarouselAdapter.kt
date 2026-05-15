package com.bottazzini.trasloco.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bottazzini.trasloco.R

class DeckCarouselAdapter(
    private val decks: List<CardDeck>,
    private val compact: Boolean = false,
    private val onDeckSelected: ((CardDeck) -> Unit)? = null,
) : RecyclerView.Adapter<DeckCarouselAdapter.DeckViewHolder>() {

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.deckItemRoot)
        val label: TextView = itemView.findViewById(R.id.deckLabel)
        val preview1: ImageView = itemView.findViewById(R.id.deckPreview1)
        val preview2: ImageView = itemView.findViewById(R.id.deckPreview2)
        val preview3: ImageView = itemView.findViewById(R.id.deckPreview3)
        val comingSoon: TextView = itemView.findViewById(R.id.deckComingSoonLabel)
    }

    override fun getItemCount(): Int = decks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck_carousel, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val deck = decks[position]
        val context = holder.itemView.context

        holder.label.text = context.getString(deck.labelRes)

        val previewNames = listOf("${deck.id}_b1", "${deck.id}_c1", "${deck.id}_d1")
        val previews = listOf(holder.preview1, holder.preview2, holder.preview3)

        previewNames.zip(previews).forEach { (name, imageView) ->
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            val drawable = if (id != 0) {
                ContextCompat.getDrawable(context, id)
            } else {
                ContextCompat.getDrawable(context, R.drawable.zero)
            }
            imageView.setImageDrawable(drawable)
        }

        if (deck.available) {
            previews.forEach { it.alpha = 1.0f }
            holder.comingSoon.visibility = View.GONE
            holder.root.isClickable = true
            holder.root.setOnClickListener { onDeckSelected?.invoke(deck) }
        } else {
            previews.forEach { it.alpha = 0.4f }
            holder.comingSoon.visibility = View.VISIBLE
            holder.root.isClickable = false
            holder.root.setOnClickListener(null)
        }

        if (compact) {
            val heightPx = (60 * context.resources.displayMetrics.density).toInt()
            previews.forEach { imageView ->
                imageView.layoutParams.height = heightPx
                imageView.requestLayout()
            }
        }
    }
}
