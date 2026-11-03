package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.TableBlock

class AddColumnUseCase {
    operator fun invoke(table: TableBlock, col: Int = -1): TableBlock {
        return table.addColumn(col)
    }
}
