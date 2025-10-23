package ru.whiteleaf.notes.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContextMenuItem

object ContextMenuHelper {

    fun showPopupMenu(
        context: Context,
        anchorView: View,
        items: List<ContextMenuItem>,
        onItemSelected: (itemId: Int) -> Unit
    ) {
        val listPopupWindow = ListPopupWindow(context).apply {

            setAdapter(ContextMenuAdapter(context, items))

            this.anchorView = anchorView
            setDropDownGravity(Gravity.START)
            width = 170.dpToPx(context)
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
            verticalOffset = -anchorView.height/2
            horizontalOffset = 1.dpToPx(context)

            setOnItemClickListener { parent, view, position, id ->
                val item = items[position]
                if (item.isEnabled) onItemSelected(item.id)
                dismiss()
            }
        }
        listPopupWindow.show()
    }

    // Пункты меню опций при просмотре записной книжки
    fun getOptionsMenuItemsNoteList(context: Context): List<ContextMenuItem> {
        return listOf(
            createMenuItem(context, R.id.options_create_note),
            createMenuItem(context, R.id.options_rename_notebook),
            createMenuItem(context, R.id.options_share_notebook),
            createMenuItem(context, R.id.options_delete_notebook)
        )
    }

    // Пункты меню опций при просмотре записной книжки
    fun getOptionsMenuItemsNoteEdit(context: Context): List<ContextMenuItem> {
        return listOf(
            createMenuItem(context, R.id.options_rename_note),
            createMenuItem(context, R.id.options_move_note),
            createMenuItem(context, R.id.options_share_note),
            createMenuItem(context, R.id.options_share_note_file),
            createMenuItem(context, R.id.options_delete_note)
        )
    }

    //пункты меню опций при получении контента
    fun getOptionsMenuShareReceiver(context: Context): List<ContextMenuItem> {
        return listOf(
            createMenuItem(context, R.id.options_save_note),
            createMenuItem(context, R.id.options_append_note),
            createMenuItem(context, R.id.options_cancel)
        )
    }

    // Пункты контекстного меню для заметок
    fun getNoteContextMenuItems(context: Context): List<ContextMenuItem> {
        return listOf(
            createMenuItem(context, R.id.note_menu_rename),
            createMenuItem(context, R.id.note_menu_move),
            createMenuItem(context, R.id.note_menu_share),
            createMenuItem(context, R.id.note_menu_delete)
        )
    }

    // Пункты контекстного меню для записных книжек
    fun getNotebookContextMenuItems(context: Context): List<ContextMenuItem> {
        return listOf(
            createMenuItem(context, R.id.note_menu_rename),
            createMenuItem(context, R.id.note_menu_share),
            createMenuItem(context, R.id.note_menu_delete)
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createMenuItem(context: Context, menuItemId: Int): ContextMenuItem {
        val menu = MenuBuilder(context)
        MenuInflater(context).inflate(R.menu.menu_items, menu)
        val item = menu.findItem(menuItemId)

        return ContextMenuItem(
            id = menuItemId,
            title = item.title.toString(),
            isEnabled = item.isEnabled
        )
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

}