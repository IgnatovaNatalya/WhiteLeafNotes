package ru.whiteleaf.notes.presentation.note_list

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.domain.model.Note
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                    .inflate(R.layout.item_start_header, parent, false)
                HeaderViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_planner_note, parent, false)
                NoteViewHolder(view, contextActionHandler)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PlannerItem.MonthHeader -> {
                (holder as HeaderViewHolder).bind(item)
            }

            is PlannerItem.NoteItem -> {
                (holder as NoteViewHolder).bind(
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
        return dateFormat.format(calendar.time).capitalize()
    }

    // ViewHolders
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMonthName: TextView = itemView.findViewById(R.id.start_header_title)

        fun bind(header: PlannerItem.MonthHeader) {
            tvMonthName.text = header.monthName
        }
    }

    class NoteViewHolder(
        itemView: View,
        private val noteActionHandler: ContextNoteActionHandler
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvDate: TextView = itemView.findViewById(R.id.tv_planer_note_date)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_planer_note_title)

        fun bind(note: Note, onClick: (Note) -> Unit) {
            val titleText = if (note.title != "") note.title else note.content.take(40)
            val isFeatured = note.title.startsWith('-')

            if (isFeatured) {
                // Особенная заметка - акцентный фон и белый текст
                itemView.background =
                    ContextCompat.getDrawable(itemView.context, R.drawable.bg_planner_note_accent)
                tvDate.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                tvTitle.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        android.R.color.white
                    )
                )
            } else {
                itemView.background =
                    ContextCompat.getDrawable(itemView.context, R.drawable.bg_planner_note)
                // Используем атрибуты темы для обычных заметок
                val typedValue = TypedValue()

                itemView.context.theme.resolveAttribute(
                    android.R.attr.textColorSecondary,
                    typedValue,
                    true
                )
                tvDate.setTextColor(typedValue.data)

                itemView.context.theme.resolveAttribute(
                    android.R.attr.textColorPrimary,
                    typedValue,
                    true
                )
                tvTitle.setTextColor(typedValue.data)

            }
            // Форматируем дату для отображения (например, "15 мар")
            tvDate.text = formatDate(note.modifiedAt)
            //tvTitle.text = note.title.trimStart('-')//.ifEmpty { "" }
            tvTitle.text = titleText.trimStart('-')

            itemView.setOnClickListener { onClick(note) }

            itemView.setOnLongClickListener {
                showContextMenu(itemView, note)
                true
            }

        }

        private fun showContextMenu(anchorView: View, note: Note) {
            ContextMenuHelper.showPopupMenu(
                context = anchorView.context,
                anchorView = anchorView,
                items = ContextMenuHelper.getNoteContextMenuItems(anchorView.context),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.note_menu_delete -> noteActionHandler.onDeleteNote(note)
                        R.id.note_menu_move -> noteActionHandler.onMoveNote(note)
                        R.id.note_menu_share -> noteActionHandler.onShareNote(note)
                        R.id.note_menu_rename -> noteActionHandler.onRenameNote(note)
                    }
                }
            )
        }

        private fun formatDate(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("dd MMM", Locale("ru"))
            return dateFormat.format(Date(timestamp))
        }
    }
}