package com.example.txtnotesapp.presentation.note_list

import androidx.recyclerview.widget.RecyclerView
import com.example.txtnotesapp.databinding.ItemNoteBinding
import com.example.txtnotesapp.domain.model.Note
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NoteViewHolder(
    private val binding: ItemNoteBinding,
    private val onNoteClicked: (Note) -> Unit,
    private val onNoteLongClicked: (Note) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(note: Note) {
        binding.noteTitle.text = note.title
        //binding.noteDate.text = formatDate(note.createdAt)

        // Превью содержимого (первые 50 символов)
        val preview = if (note.content.length > 50) {
            note.content.substring(0, 50) + "..."
        } else {
            note.content
        }
        //binding.notePreview.text = preview

        // Обработка кликов
        binding.root.setOnClickListener {
            onNoteClicked(note)
        }

        binding.root.setOnLongClickListener {
            onNoteLongClicked(note)
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