package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.TableBlock

class DeleteColumnUseCase {
    operator fun invoke(table: TableBlock, col: Int): TableBlock {
        return table.deleteColumn(col)
    }
}
