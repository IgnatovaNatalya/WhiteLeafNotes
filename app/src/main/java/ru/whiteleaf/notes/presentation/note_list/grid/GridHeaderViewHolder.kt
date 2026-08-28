package ru.whiteleaf.notes.presentation.note_list.grid

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R

class GridHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvMonthName: TextView = itemView.findViewById(R.id.grid_header_title)

    fun bind(header: PlannerItem.MonthHeader) {
        tvMonthName.text = header.monthName
    }
}