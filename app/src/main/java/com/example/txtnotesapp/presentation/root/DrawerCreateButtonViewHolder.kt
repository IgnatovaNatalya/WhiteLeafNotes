package com.example.txtnotesapp.presentation.root

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class DrawerCreateButtonViewHolder(view: View, private val onButtonClicked: () -> Unit) :
    RecyclerView.ViewHolder(view) {
    fun bind(title: String) {
        itemView.setOnClickListener { onButtonClicked() }
    }
}