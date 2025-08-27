package com.example.txtnotesapp.presentation.root

import androidx.recyclerview.widget.DiffUtil

class DrawerMenuDiffCallback : DiffUtil.ItemCallback<DrawerMenuItem>() {

    override fun areItemsTheSame(oldItem: DrawerMenuItem, newItem: DrawerMenuItem): Boolean {
        // Проверяем, ссылаются ли оба элемента на один и тот же объект
        return when {
            oldItem is DrawerMenuItem.NotebookItem && newItem is DrawerMenuItem.NotebookItem ->
                oldItem.notebook.path == newItem.notebook.path
            oldItem is DrawerMenuItem.NoteItem && newItem is DrawerMenuItem.NoteItem ->
                oldItem.note.title == newItem.note.title
//            oldItem is DrawerMenuItem.Header && newItem is DrawerMenuItem.Header ->
//                oldItem.title == newItem.title
            else -> oldItem::class == newItem::class
        }
    }

    override fun areContentsTheSame(oldItem: DrawerMenuItem, newItem: DrawerMenuItem): Boolean {
        // Проверяем, имеют ли элементы одинаковое содержимое
        return when {
            oldItem is DrawerMenuItem.NotebookItem && newItem is DrawerMenuItem.NotebookItem ->
                oldItem.notebook == newItem.notebook
            oldItem is DrawerMenuItem.NoteItem && newItem is DrawerMenuItem.NoteItem ->
                oldItem.note == newItem.note
//            oldItem is DrawerMenuItem.Header && newItem is DrawerMenuItem.Header ->
//                oldItem.title == newItem.title
            else -> true // Для кнопок, разделителей и пустых состояний считаем содержимое одинаковым
        }
    }
}