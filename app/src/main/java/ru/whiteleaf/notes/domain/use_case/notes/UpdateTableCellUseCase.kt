package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.TableBlock

class UpdateTableCellUseCase {
    operator fun invoke(table: TableBlock, row: Int, col: Int, text: String): TableBlock {
        return table.setCell(row, col, text)
    }
}
