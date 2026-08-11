package ru.whiteleaf.notes.common.utils

import ru.whiteleaf.notes.domain.model.Notebook

sealed class DropdownNotebookItem {
    abstract fun toDisplayString(): String
    abstract fun toLabelString(): String

    data class NotebookItem(val notebook: Notebook) : DropdownNotebookItem() {
        override fun toDisplayString(): String = notebook.path
        override fun toLabelString(): String =
            "Переместить в «${if (notebook.path.length > 10) notebook.path.take(10) + "..." else notebook.path}»"

    }

    object RootItem : DropdownNotebookItem() {
        override fun toDisplayString(): String = "/ Корневая папка"
        override fun toLabelString(): String = "Переместить в корень"
    }
}