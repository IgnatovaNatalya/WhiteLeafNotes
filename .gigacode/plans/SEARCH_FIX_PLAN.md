# План исправления поиска в редакторе заметок

## ✅ Проблема решена!

При поиске в редакторе заметок приложение падало с `IndexOutOfBoundsException`, потому что `match.start` (позиция в контенте) применялась к `titleEditText` (заголовок).

## Что было исправлено

### ✅ Шаг 1: Добавить метод подсветки в TextBlockViewHolder
**Файл:** `TextBlockViewHolder.kt` - DONE
- ✅ Добавить метод `highlightText(start: Int, end: Int, color: Int)`
- ✅ Использовать `SpannableString` с `BackgroundColorSpan`
- ✅ Метод `clearHighlight()` для удаления подсветки

### ✅ Шаг 2: Добавить поддержку подсветки в ContentBlockAdapter
**Файл:** `ContentBlockAdapter.kt` - DONE
- ✅ Добавить метод `highlightMatch(match: SearchMatch, color: Int, recyclerView: RecyclerView)`
- ✅ Метод должен найти ViewHolder по позиции и вызвать `highlightText()`
- ✅ Добавить метод `clearAllHighlights(recyclerView: RecyclerView)` для очистки всех подсветок
- ✅ Добавить метод `findContentBlockPosition(contentOffset: Int)` для поиска блока по смещению

### ✅ Шаг 3: Исправить renderSearchResults в NoteEditFragment
**Файл:** `NoteEditFragment.kt` - DONE
- ✅ Проверить `match.target`:
  - Если `TITLE`: применить `setSelection()` к `titleEditText` с проверкой границ
  - Если `CONTENT`: найти TextBlock в RecyclerView, подсветить и проскроллить
- ✅ Добавить логику скролла к содержимому при match в контенте (`scrollToContentMatch()`)
- ✅ Обновить `clearHighlights()` для очистки подсветок в заголовке и контенте

### ✅ Шаг 4: Навигация по matches
**Файл:** `NoteEditViewModel.kt` - DONE
- ✅ Методы `nextMatch()` и `previousMatch()` уже работают корректно
- ✅ `postNoteAndSearch()` передаёт правильный `currentIndex`
- ✅ При каждом переключении match вызывается `renderSearchResults()` который очищает старую подсветку и подсвечивает новый

## Ключевые решения

1. **Позиции относительные**: `match.start` в заголовке — это позиция в строке заголовка, в контенте — позиция в полном тексте контента
2. **Скролл к match**: при CONTENT match находится TextBlock и NestedScrollView проскроллиется к нему
3. **Очистка подсветки**: `clearAllHighlights()` вызывается перед подсветкой нового match
4. **Безопасность**: проверки границ `coerceIn()` перед установкой selection

## 📋 Оставлено на будущее

### Подсветка для ImageBlock и TableBlock
- **ImageBlockViewHolder**: подсветить caption EditText
- **TableBlockViewHolder**: подсветить ячейки, содержащие match
- **TODO**: реализовать при необходимости
