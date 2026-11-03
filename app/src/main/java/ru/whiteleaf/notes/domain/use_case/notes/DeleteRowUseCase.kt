package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.TableBlock

class DeleteRowUseCase {
    operator fun invoke(table: TableBlock, row: Int): TableBlock {
        return table.deleteRow(row)
    }
}
