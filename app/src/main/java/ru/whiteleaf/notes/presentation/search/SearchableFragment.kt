package ru.whiteleaf.notes.presentation.search

interface SearchableFragment {
    fun onSearchQueryChanged(query: String)   // вызывается при каждом изменении текста
    fun onSearchQuerySubmitted(query: String) // вызывается при нажатии "Поиск" на клавиатуре
}