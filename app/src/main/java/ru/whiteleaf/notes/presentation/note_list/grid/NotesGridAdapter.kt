package ru.whiteleaf.notes.presentation.note_list.grid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.domain.model.Note
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Locale.getDefault

// Модели для адаптера
sealed class PlannerItem {
    data class MonthHeader(val monthName: String, val year: Int) : PlannerItem()
    data class NoteItem(val note: Note) : PlannerItem()
}

class NotesGridAdapter(
    private val onNoteClickListener: (Note) -> Unit,
    private val contextActionHandler: ContextNoteActionHandler
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<PlannerItem> = emptyList()

    val TYPE_HEADER = 0
    val TYPE_NOTE = 1

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PlannerItem.MonthHeader -> TYPE_HEADER
            is PlannerItem.NoteItem -> TYPE_NOTE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_grid_header, parent, false)
                GridHeaderViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_planner_note, parent, false)
                NoteInGridViewHolder(view, contextActionHandler)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PlannerItem.MonthHeader -> {
                (holder as GridHeaderViewHolder).bind(item)
            }

            is PlannerItem.NoteItem -> {
                (holder as NoteInGridViewHolder).bind(
                    item.note,
                    onNoteClickListener,
                )
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(notes: List<Note>) {
        this.items = groupNotesByMonth(notes)
        notifyDataSetChanged()
    }

    private fun groupNotesByMonth(notes: List<Note>): List<PlannerItem> {
        if (notes.isEmpty()) return emptyList()

        val result = mutableListOf<PlannerItem>()

        // Группируем заметки по году и месяцу
        val grouped = notes.sortedBy { it.modifiedAt }
            .groupBy { note ->
                val calendar = Calendar.getInstance().apply { timeInMillis = note.modifiedAt }
                Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
            }
            .toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

        // Создаем список с хедерами и заметками
        grouped.forEach { (yearMonth, notesInMonth) ->
            val (year, month) = yearMonth
            val monthName = getMonthName(month, year)

            // Добавляем header месяца
            result.add(PlannerItem.MonthHeader(monthName, year))

            // Добавляем все заметки месяца
            result.addAll(notesInMonth.map { PlannerItem.NoteItem(it) })
        }

        return result
    }

    private fun getMonthName(month: Int, year: Int): String {
        val dateFormat = SimpleDateFormat("LLLL yyyy", Locale("ru"))
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        return dateFormat.format(calendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
    }
}