package com.example.txtnotesapp.presentation.start

import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.R

class AddButtonViewHolder(
    view: View,
    private val onAddClicked: () -> Unit,
    private val buttonText: String
) : RecyclerView.ViewHolder(view) {
    private val button: Button = view.findViewById(R.id.addButton)

    fun bind() {
        button.text = buttonText
        button.setOnClickListener {
            onAddClicked()
        }
    }
}