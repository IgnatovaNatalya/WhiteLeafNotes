package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R

class DividerAfterViewHolder(
    view: View
) : RecyclerView.ViewHolder(view) {
    private val button: ImageButton = view.findViewById(R.id.btn_more)
    fun bind() {
        button.visibility = View.GONE
    }
}