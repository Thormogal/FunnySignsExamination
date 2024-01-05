package com.example.funnysignsexamination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class Adapter(private val signs: MutableList<Sign>) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.signImage)
        val textView: TextView = itemView.findViewById(R.id.signText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.funny_sign_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSign = signs[position]

        if (currentSign.imageUrl.isNotEmpty()) {
            holder.imageView.visibility = View.VISIBLE
            holder.textView.visibility = View.GONE
            Picasso.get().load(currentSign.imageUrl).into(holder.imageView)
        } else {
            holder.imageView.visibility = View.GONE
            holder.textView.visibility = View.VISIBLE
            holder.textView.text = holder.textView.context.getString(R.string.error_loading_text)
        }
    }

    override fun getItemCount(): Int {
        return signs.size
    }

    fun updateData(newSigns: List<Sign>) {
        signs.clear()
        signs.addAll(newSigns)
        notifyDataSetChanged()
    }
}