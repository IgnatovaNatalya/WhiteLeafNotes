package com.example.whiteleafnotes.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.whiteleafnotes.R

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.title)

    fun bind(headerTitle: String) {
        title.text = headerTitle
    }
}