package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.TableBlock

class AddRowUseCase {
    operator fun invoke(table: TableBlock, row: Int = -1, isHeader: Boolean = false): TableBlock {
        return table.addRow(row, isHeader)
    }
}
