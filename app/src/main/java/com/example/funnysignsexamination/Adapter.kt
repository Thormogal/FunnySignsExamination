package com.example.funnysignsexamination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class Adapter(private val signs: MutableList<Sign>) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.funny_sign_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSign = signs[position]
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