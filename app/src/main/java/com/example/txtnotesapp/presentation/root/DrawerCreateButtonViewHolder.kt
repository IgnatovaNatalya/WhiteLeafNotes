package com.example.txtnotesapp.presentation.root

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.example.txtnotesapp.R

class DrawerCreateButtonViewHolder(view: View, private val onButtonClicked: () -> Unit) :
    RecyclerView.ViewHolder(view) {
    fun bind(title: String) {
        val button = itemView.findViewById<TextView>(R.id.drawer_create_button)
        button.text = title
        itemView.setOnClickListener { onButtonClicked() }
    }
}