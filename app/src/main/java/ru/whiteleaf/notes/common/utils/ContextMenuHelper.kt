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
        val mutableItems = items.toMutableList()
        val adapter = ContextMenuAdapter(context, mutableItems)

        val listPopupWindow = ListPopupWindow(context).apply {

            setAdapter(adapter)

            this.anchorView = anchorView
            setDropDownGravity(Gravity.END)
            //setDropDownGravity(Gravity.START)
            width = 195.dpToPx(context)
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
            verticalOffset = -anchorView.height / 2
            horizontalOffset = 12.dpToPx(context)

            setOnItemClickListener { _, _, position, _ ->
                val item = adapter.getItem(position)
                if (item.subItems != null) {
                    adapter.toggleExpand(item)
                } else {
                    onItemSelected(item.id)
                    dismiss()
                }
            }
        }
        listPopupWindow.show()
    }

    // Пункты меню опций записной книжки
    fun getOptionsMenuItemsNoteList(
        context: Context,
        isProtected: Boolean,
        isPlannerView: Boolean
    ): List<ContextMenuItem> {

        return listOf(
            createMenuItem(context, R.id.options_create_note, true),
            createSubMenu(
                context, R.id.options_view_mode, listOf(
                    createCheckableMenuItem(context, R.id.options_view_list, !isPlannerView),
                    createCheckableMenuItem(context, R.id.options_view_planner, isPlannerView, true )
                ),
                true
            ),
            createMenuItem(context, R.id.options_rename_notebook),

            if (isProtected)
                createMenuItem(context, R.id.options_unprotect_notebook)
            else
                createMenuItem(context, R.id.options_protect_notebook),
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
    private fun createMenuItem(
        context: Context,
        menuItemId: Int,
        isLastInGroup: Boolean = false
    ): ContextMenuItem {
        val menu = MenuBuilder(context)
        MenuInflater(context).inflate(R.menu.menu_items, menu)
        val item = menu.findItem(menuItemId)

        return ContextMenuItem(
            id = menuItemId,
            title = item.title.toString(),
            isEnabled = item.isEnabled,
            iconRes = item.icon,
            isLastInGroup = isLastInGroup
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createCheckableMenuItem(
        context: Context,
        menuItemId: Int,
        isChecked: Boolean,
        isLastInGroup: Boolean = false
    ): ContextMenuItem {
        val menu = MenuBuilder(context)
        MenuInflater(context).inflate(R.menu.menu_items, menu)
        val item = menu.findItem(menuItemId)

        return ContextMenuItem(
            id = menuItemId,
            title = item.title.toString(),
            isEnabled = item.isEnabled,
            isChecked = isChecked,
            isLastInGroup = isLastInGroup
        )
    }


    @SuppressLint("RestrictedApi")
    private fun createSubMenu(
        context: Context,
        menuItemId: Int,
        items: List<ContextMenuItem>,
        isLastInGroup: Boolean = false
    ): ContextMenuItem {
        val menu = MenuBuilder(context)
        MenuInflater(context).inflate(R.menu.menu_items, menu)
        val item = menu.findItem(menuItemId)

        return ContextMenuItem(
            id = menuItemId,
            title = item.title.toString(),
            isEnabled = item.isEnabled,
            subItems = items,
            isLastInGroup = isLastInGroup
        )
    }


    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}