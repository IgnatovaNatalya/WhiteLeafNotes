package com.example.txtnotesapp.presentation.note_list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.databinding.ItemNoteBinding
import com.example.txtnotesapp.domain.model.Note

class NoteViewHolder(
    private val binding: ItemNoteBinding,
    private val onNoteClicked: (Note) -> Unit,
    private val onNoteLongClicked: (View, Note) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(note: Note) {
        binding.noteTitle.text = note.title

        // Обработка кликов
        binding.root.setOnClickListener {
            onNoteClicked(note)
        }

//        binding.root.setOnLongClickListener {
//            onNoteLongClicked(note)
//            true
//        }

        binding.root.setOnLongClickListener {
            onNoteLongClicked(binding.root, note)  // Передаем view и note
            true
        }
    }

//    private fun formatDate(timestamp: Long): String {
//        val date = Instant.fromEpochMilliseconds(timestamp)
//            .toLocalDateTime(TimeZone.currentSystemDefault())
//
//        return "${date.dayOfMonth} ${getMonthName(date.month)} ${date.year}"
//    }

//    private fun getMonthName(month: Month): String { //todo переместить во фррагмент редактирования
//        return when (month) {
//            Month.JANUARY -> "Января"
//            Month.FEBRUARY -> "Февраля"
//            Month.MARCH -> "Марта"
//            Month.APRIL -> "Апреля"
//            Month.MAY -> "Мая"
//            Month.JUNE -> "Июня"
//            Month.JULY -> "Июля"
//            Month.AUGUST -> "Августа"
//            Month.SEPTEMBER -> "Сентября"
//            Month.OCTOBER -> "Октября"
//            Month.NOVEMBER -> "Ноября"
//            Month.DECEMBER -> "Декабря"
//        }
//    }
}