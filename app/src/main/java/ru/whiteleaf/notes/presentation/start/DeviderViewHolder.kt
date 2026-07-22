package ru.whiteleaf.notes.presentation.start

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R

class DividerViewHolder(
    view: View,
    private val onMoreClicked: () -> Unit,
) : RecyclerView.ViewHolder(view) {
    private val button = view.findViewById<ImageButton>(R.id.divider_button_more)

    fun bind(isMore: Boolean) {
        if (isMore) {
            button.visibility = View.VISIBLE
            button.setOnClickListener {
                onMoreClicked()
            }
        } else {
            button.visibility = View.GONE
        }
    }

}