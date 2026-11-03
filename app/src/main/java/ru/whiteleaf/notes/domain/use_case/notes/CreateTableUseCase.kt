package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.TableBlock

class CreateTableUseCase {
    operator fun invoke(rows: Int, cols: Int): TableBlock {
        val headerCells = List(cols) { ru.whiteleaf.notes.domain.model.TableCell("", isHeader = true) }
        val dataRows = List(rows - 1) { List(cols) { ru.whiteleaf.notes.domain.model.TableCell("") } }
        return TableBlock(listOf(headerCells) + dataRows)
    }
}
