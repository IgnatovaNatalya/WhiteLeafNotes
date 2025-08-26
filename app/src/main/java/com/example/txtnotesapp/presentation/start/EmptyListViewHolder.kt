package com.example.txtnotesapp.presentation.start

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R

class EmptyListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val text: TextView = view.findViewById(R.id.start_empty_list)

    fun bind(message: String) {
        text.text = message
    }
}