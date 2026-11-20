package ru.whiteleaf.notes.presentation.note_edit


import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.ContentBlockParser
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.RecyclerViewScrollManager
import ru.whiteleaf.notes.common.utils.syncTextsFromRecyclerView
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.databinding.FragmentNoteEditBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.whiteleaf.notes.common.utils.DialogHelper.createChangeDateDialog
import ru.whiteleaf.notes.common.utils.formatDate
import ru.whiteleaf.notes.common.utils.hideKeyboard
import ru.whiteleaf.notes.common.utils.toggleSecurePreview
import ru.whiteleaf.notes.domain.model.ContentBlock
import ru.whiteleaf.notes.presentation.note_edit.adapter.ContentBlockAdapter
import ru.whiteleaf.notes.presentation.note_edit.adapter.TextBlockViewHolder
import ru.whiteleaf.notes.presentation.root.RootActivity
import ru.whiteleaf.notes.presentation.root.RootViewModel
import ru.whiteleaf.notes.presentation.search.SearchableFragment
import kotlin.getValue

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>(), SearchableFragment {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath, args.searchQuery, args.contentPosition)
    }

    private val rootViewModel: RootViewModel by activityViewModel()

    private val args: NoteEditFragmentArgs by navArgs()

    private var isExternalSearch = false

    private var isEditing = false
    private var notSaveOnPause = false
    private var wasInterrupted = false

    private lateinit var titleEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var noteScrollView: NestedScrollView
    private lateinit var noteBlocked: LinearLayout
    private lateinit var noteBlockedUnsaved: LinearLayout
    private lateinit var btnLockIndicator: ImageButton
    private lateinit var searchView: SearchView
    private lateinit var optionsButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var nextButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var llMatchButtons: LinearLayout

    private lateinit var blockAdapter: ContentBlockAdapter

    private var windowFocusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null
    private var focusObserver: ViewTreeObserver? = null

    private var highlightColor = 0

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNoteEditBinding {
        return FragmentNoteEditBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleEditText = binding.noteEditTitle
        buttonScroll = binding.noteScrollDown
        noteScrollView = binding.noteEditScrollView
        noteBlocked = binding.llBlocked
        noteBlockedUnsaved = binding.llBlockedUnsaved
        progressBar = binding.noteEditProgressBar
        nextButton = binding.nextMatch
        prevButton = binding.prevMatch
        llMatchButtons = binding.llPrevNextButtons

        btnLockIndicator = requireActivity().findViewById(R.id.btn_lock_indicator)
        optionsButton = requireActivity().findViewById(R.id.btn_options_menu)
        searchView = requireActivity().findViewById(R.id.search_view)
        highlightColor = ContextCompat.getColor(requireContext(), R.color.blue_transparent)

        if (args.searchQuery != null) isExternalSearch = true

        setupBlockAdapter()
        setupSecurityPreview()
        setupWindowFocusChangeListener(view)
        setupOptionsMenu()
        setupObservers()
        setupScrollDown()
        setupEditTexts()
        setupClickListeners()
        setupBackCallback()
        setupSearchView()
    }

    private fun setupBlockAdapter() {
        blockAdapter = ContentBlockAdapter()
        binding.noteEditRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.noteEditRecycler.adapter = blockAdapter

        // Устанавливаем callback для сохранения заметки
        // Debounce (500мс) происходит автоматически внутри adapter при каждом изменении текста
        blockAdapter.setOnNoteSaved {
            val content = blockAdapter.currentList.joinToString("\n\n") { block ->
                when (block) {
                    is ContentBlock.Text -> block.text
                    is ContentBlock.Image -> block.imagePath
                    is ContentBlock.Table -> block.rows.joinToString("\n") { it.joinToString("|") }
                }
            }
            viewModel.updateNoteContent(content)
        }
    }

    private fun setupSearchView() {
        searchView.queryHint = "Поиск по заметке"
    }

    override fun onBackPressed(): Boolean {
        println("DEBUG: NoteEditFragment: onBackPressed: isSearching=${rootViewModel.isSearching()}, external search query=${args.searchQuery}")
        if (rootViewModel.isSearching()) {
            if (isExternalSearch) {
                return false //Будет обработано в activity
            } else {
                (requireActivity() as RootActivity).cancelSearch()
                isExternalSearch = false
                return true  // событие обработано, навигацию не трогаем
            }
        } else {
            return false
        }
    }

    private fun setupBackCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            println("DEBUG: NoteEditFragment: BackCallback: isSearching=${rootViewModel.isSearching()}, external search query=${args.searchQuery}")
            if (rootViewModel.isSearching()) {
                if (isExternalSearch) {
                    findNavController().navigateUp()
                } else {
                    (requireActivity() as RootActivity).cancelSearch()
                    isExternalSearch = false
                }
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupSecurityPreview() {
        toggleSecurePreview(requireActivity(), viewModel.getEncryptionStatus())
    }

    private fun setupWindowFocusChangeListener(view: View) {
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            onFocusChanged(hasFocus)
        }
        focusObserver = observer
        windowFocusListener = listener
        observer.addOnWindowFocusChangeListener(listener)
    }

    fun onFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            //при переходе фокуса, клавиатура скрывается системой

            viewModel.updateNoteTitleIfChanged(titleEditText.text.toString()) //Обновляем только заголовок, т.к. контент и так сохраняется при любом изменении
            viewModel.rememberNoteScrollPosition(noteScrollView.scrollY)
            wasInterrupted = true
            println("DEBUG: NoteEditFragment: Focus cleared")
        } else if (wasInterrupted) {
            wasInterrupted = false
            //contentEditText.clearFocus() //снимаем фокус с контента, чтобы клавиатура могла включиться потом

            println("DEBUG: NoteEditFragment: Window focus received")
            viewModel.reloadNotePosition()
        }
    }

    private fun setupObservers() {
        viewModel.noteEditState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            renderEvent(event)
        }
    }

    private fun setupScrollDown() {
        // Scroll detection for RecyclerView
        RecyclerViewScrollManager.setupScrollDetection(
            recyclerView = binding.noteEditRecycler,
            scrollView = noteScrollView,
            button = buttonScroll
        )
        
        // Также обновляем scroll position при скролле
        noteScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewModel.rememberNoteScrollPosition(scrollY)
        }
    }

    private fun setupEditTexts() {
        // Обновляем состояние режима редактирования
//        binding.noteEditRecycler.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
//            override fun onChildViewAttachedToWindow(view: View) {
//                if (view is EditText) {
//                    RecyclerViewScrollManager.isEditingMode = true
//                }
//            }
//
//            override fun onChildViewDetachedFromWindow(view: View) {
//                if (view is EditText) {
//                    RecyclerViewScrollManager.isEditingMode = false
//                }
//            }
//        })
    }

    private fun setupClickListeners() {

        binding.noteEditDate.setOnClickListener { changeNoteDate() }

        binding.unlockButton.setOnClickListener {
            viewModel.unlockNote(requireContext())
        }

        binding.unlockAndSaveButton.setOnClickListener {
            viewModel.unlockAndSavePendingContent(requireContext())
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }

        nextButton.setOnClickListener { viewModel.nextMatch() }
        prevButton.setOnClickListener { viewModel.previousMatch() }
    }

    override fun onSearchQueryChanged(query: String) {
        //не ищем пока не нажмет сабмит
        titleEditText.clearFocus()
        //contentEditText.clearFocus()
        clearHighlights()
        viewModel.onSearchCleared()

        if (query == "") {
            isExternalSearch = false
        }
    }

    /**
     * Очищает все подсветки (заголовок и контент).
     */
    private fun clearHighlights() {
        // Очищаем заголовок
        val titleText = titleEditText.text as? Spannable ?: return
        titleText.getSpans(0, titleText.length, BackgroundColorSpan::class.java)
            .forEach { titleText.removeSpan(it) }
        
        // Очищаем контент в RecyclerView
        blockAdapter.clearAllHighlights(binding.noteEditRecycler)
        
        // Снимаем выделение с заголовка
        titleEditText.setSelection(0)
    }

    override fun onSearchQuerySubmitted(query: String) {
        viewModel.onSearchQuerySubmitted(query)
        isExternalSearch = false
    }

    override fun onSearchCleared() {
        //clearHighlights()
        viewModel.onSearchCleared()
        isExternalSearch = false
    }

    override fun onSearchStarted() {}

    fun changeNoteDate() {
        val note = viewModel.getNote() ?: return
        createChangeDateDialog(
            note.modifiedAt,
            onDateSelected = { date -> viewModel.updateNoteDate(date) }
        ).show(childFragmentManager, "date_picker")
    }

    private fun setupOptionsMenu() {

        optionsButton.setOnClickListener {
            titleEditText.clearFocus()
            //contentEditText.clearFocus()
            ContextMenuHelper.showPopupMenu(
                context = requireContext(),
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuItemsNoteEdit(optionsButton.context),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_rename_note -> onOptionsRenameNote()
                        R.id.options_change_note_date -> onOptionsChangeNoteDate()
                        R.id.options_move_note -> onOptionsMoveNote()
                        R.id.options_share_note -> onOptionsShareNote()
                        R.id.options_share_note_file -> onOptionsShareNoteFile()
                        R.id.options_delete_note -> onOptionsDeleteNote()
                    }
                }
            )
        }
    }

    private fun onOptionsRenameNote() {
        titleEditText.requestFocus()
    }

    private fun onOptionsChangeNoteDate() = changeNoteDate()

    private fun onOptionsMoveNote() {
        DialogHelper.createMoveNoteDialog(
            requireContext(),
            viewModel.getAllNotebooks(),
            args.notebookPath ?: "",
        ) { targetNotebookPath ->
            viewModel.moveNote(requireContext(), targetNotebookPath)
        }.show()
    }

    private fun onOptionsShareNote() {
        viewModel.shareNote(requireContext())
    }

    private fun onOptionsShareNoteFile() {
        if (noteIsNotEmpty()) viewModel.shareFile(requireContext())
        else renderMessage("Пустая заметка")
    }

    private fun onOptionsDeleteNote() {
        DialogHelper.createDeleteNoteConfirmationDialog(
            requireContext(),
            titleEditText.text.toString()
        )
        { viewModel.deleteNote() }.show()
    }

    private fun renderState(state: NoteEditState) {
        when (state) {

            is NoteEditState.Success -> {
                val st =
                    if (state.searchState == null) "search state is null" else state.searchState.debugString()
                println("DEBUG: NoteEditFragment: Rendering note, $st, isEditing=$isEditing")
                noteScrollView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                searchView.visibility = View.VISIBLE

                if (state.isEncrypted) {
                    btnLockIndicator.setImageResource(R.drawable.ic_ind_unlocked)
                    btnLockIndicator.visibility = View.VISIBLE
                    btnLockIndicator.setOnClickListener(null)
                    btnLockIndicator.setOnClickListener { viewModel.lockNote() }
                } else
                    btnLockIndicator.visibility = View.GONE

                val note = state.note
                titleEditText.setText(note.title)
                binding.noteEditDate.text = formatDate(note.modifiedAt)

                val searchState = state.searchState

                // ВСЕГДА заполняем blockAdapter блоками из контента
                // Это нужно для корректного сохранения при onPause
                val blocks = ContentBlockParser.parse(note.content).takeIf { it.isNotEmpty() }
                    ?: listOf(ContentBlock.Text(""))
                blockAdapter.submitList(blocks)
                isEditing = false

                if (searchState != null) {
                    //результаты поиска
                    optionsButton.visibility = View.GONE
                    println("DEBUG: NoteEditFragment: rendering matches")
                    renderSearchResults(searchState)
                } else {
                    //Обычный вид
                    optionsButton.visibility = View.VISIBLE
                    println("DEBUG: NoteEditFragment: rendering note no search")

                    noteScrollView.post { noteScrollView.scrollTo(0, state.scrollPosition) }
                    llMatchButtons.visibility = View.GONE
                }
            }

            NoteEditState.Loading -> {
                println("DEBUG: NoteEditFragment: Rendering loading")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
                searchView.visibility = View.GONE
                optionsButton.visibility = View.GONE
                llMatchButtons.visibility = View.GONE
            }

            is NoteEditState.Error -> {
                println("DEBUG: NoteEditFragment: Rendering error")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
                searchView.visibility = View.GONE
                optionsButton.visibility = View.GONE
                buttonScroll.visibility = View.GONE
                renderMessage(state.message)
                llMatchButtons.visibility = View.GONE
            }

            is NoteEditState.Blocked -> {
                println("DEBUG: NoteEditFragment: Rendering blocked hasChanges=${state.hasUnsavedChanges}")
                hideKeyboard(titleEditText)

                progressBar.visibility = View.GONE

                if (state.hasUnsavedChanges) {
                    noteBlocked.visibility = View.GONE
                    noteBlockedUnsaved.visibility = View.VISIBLE
                } else {
                    noteBlocked.visibility = View.VISIBLE
                    noteBlockedUnsaved.visibility = View.GONE
                }
                noteScrollView.visibility = View.GONE
                btnLockIndicator.setImageResource(R.drawable.ic_ind_locked)
                btnLockIndicator.visibility = View.VISIBLE
                searchView.visibility = View.GONE
                optionsButton.visibility = View.GONE
                llMatchButtons.visibility = View.GONE
            }
        }
    }



    private fun renderSearchResults(state: SearchState) {
        val note = viewModel.getNote() ?: return

        // Устанавливаем заголовок без подсветки
        titleEditText.setText(note.title, TextView.BufferType.SPANNABLE)

        // Подсвечиваем все TITLE match одним цветом
        highlightAllTitleMatches(state)

        // Подсвечиваем все CONTENT match одним цветом
        val allMatches = state.matches
        if (allMatches.isNotEmpty()) {
            // Очищаем все подсветки контента
            blockAdapter.clearAllHighlights(binding.noteEditRecycler)
            
            // Подсвечиваем ВСЕ CONTENT match одним цветом
            highlightAllContentMatches(allMatches, highlightColor)
            
            // Получаем текущий match
            val currentMatch = allMatches[state.currentMatchIndex]
            
            // Скроллим к текущему match
            scrollToCurrentMatch(currentMatch)
            
            // Устанавливаем selection в заголовке если match в заголовке
            // Текущее вхождение выделяется через setSelection (системная подсветка)
            if (currentMatch.target == SearchMatchTarget.TITLE) {
                titleEditText.post {
                    val safeStart = currentMatch.start.coerceIn(0, titleEditText.text.length)
                    val safeEnd = currentMatch.end.coerceIn(0, titleEditText.text.length)
                    if (safeStart < safeEnd) {
                        titleEditText.setSelection(safeStart, safeEnd)
                    }
                }
            }
        }

        buttonScroll.visibility = View.GONE
        llMatchButtons.visibility = if (allMatches.size > 1) View.VISIBLE else View.GONE
        prevButton.isEnabled = state.currentMatchIndex > 0
        nextButton.isEnabled = state.currentMatchIndex < allMatches.lastIndex
    }

    /**
     * Подсвечивает все CONTENT match одним цветом.
     */
    private fun highlightAllContentMatches(matches: List<SearchMatch>, color: Int) {
        // Очищаем все подсветки сначала
        blockAdapter.clearAllHighlights(binding.noteEditRecycler)
        
        // Подсвечиваем все CONTENT match
        for (match in matches) {
            if (match.target == SearchMatchTarget.CONTENT) {
                blockAdapter.highlightMatch(match, color, binding.noteEditRecycler)
            }
        }
    }

    /**
     * Подсвечивает все TITLE match одним цветом.
     * Текущий TITLE match выделяется через setSelection (системная подсветка).
     */
    private fun highlightAllTitleMatches(state: SearchState) {
        val text = titleEditText.text ?: return
        val spannable = android.text.SpannableString(text)
        
        // Удаляем старую подсветку
        spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
            .forEach { spannable.removeSpan(it) }
        
        // Подсвечиваем все TITLE match одним цветом
        for ((start, end, target) in state.matches) {
            if (target == SearchMatchTarget.TITLE) {
                if (start >= 0 && end <= spannable.length && start < end) {
                    spannable.setSpan(
                        BackgroundColorSpan(highlightColor),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        
        titleEditText.setText(spannable, TextView.BufferType.SPANNABLE)
    }

    /**
     * Скроллит к текущему match.
     */
    private fun scrollToCurrentMatch(match: SearchMatch) {
        if (match.target == SearchMatchTarget.CONTENT) {
            scrollToContentMatch(match)
        }
    }

    /**
     * Скроллит к TextBlock, содержащему match в контенте.
     */
    private fun scrollToContentMatch(match: SearchMatch) {
        // Находим позицию блока в RecyclerView
        var currentOffset = 0
        for (i in 0 until blockAdapter.itemCount) {
            val block = blockAdapter.getItem(i)
            if (block is ContentBlock.Text) {
                val blockLength = block.text.length + 2 // +2 для \n\n
                if (match.start in currentOffset until currentOffset + block.text.length) {
                    // Нашли блок — скроллим к нему
                    binding.noteEditRecycler.post {
                        val holder = binding.noteEditRecycler.findViewHolderForAdapterPosition(i)
                        if (holder is TextBlockViewHolder) {
                            // Скроллим NestedScrollView к этому блоку
                            val targetY = holder.itemView.top + noteScrollView.scrollY
                            noteScrollView.smoothScrollTo(0, targetY)
                            
                            // Устанавливаем курсор на позицию match
                            val localOffset = match.start - currentOffset
                            holder.highlightText(match.start, match.end, highlightColor)
                            holder.getEditText()?.setSelection(localOffset)
                        }
                    }
                    return
                }
                currentOffset += blockLength
            }
        }
    }

    private fun renderEvent(event: NoteEditNavigationEvent?) {
        if (event == null) return

        println("DEBUG: NoteEditFragment: renderEvent: event=$event")

        when (event) {
            NoteEditNavigationEvent.NavigateBack -> {
                println("DEBUG: NoteEditFragment: NavigateBack")
                notSaveOnPause = true
                findNavController().popBackStack()
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShareFile -> {
                ShareHelper.shareFile(requireContext(), event.uri)
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShareNote -> {
                ShareHelper.shareNote(requireContext(), event.note)
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShowMessage -> {
                renderMessage(event.message)
                viewModel.clearEvent()
            }

            NoteEditNavigationEvent.ShowBiometric -> {
                viewModel.unlockAndLoad(requireContext())
                viewModel.clearEvent()
            }
        }
    }

    private fun renderMessage(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun noteIsNotEmpty() =
        titleEditText.text.toString().trim() != "" || blockAdapter.currentList.any { 
            it is ContentBlock.Text && it.text.isNotBlank() 
        }

    override fun onPause() {
        super.onPause()

        if (!notSaveOnPause) {
            // Синхронизируем текст из всех EditText в RecyclerView
            binding.noteEditRecycler.syncTextsFromRecyclerView(blockAdapter)
            // Сохраняем заголовок и контент атомарно через UpdateFullNoteUseCase
            val newTitle = titleEditText.text.toString()
            val content = blockAdapter.currentList.joinToString("\n\n") { block ->
                when (block) {
                    is ContentBlock.Text -> block.text
                    is ContentBlock.Image -> block.imagePath
                    is ContentBlock.Table -> block.rows.joinToString("\n") { it.joinToString("|") }
                }
            }
            println("DEBUG: NoteEditFragment: onPause saving title='$newTitle', content='$content'")
            viewModel.updateNoteInFull(newTitle, content)
            viewModel.saveNoteScrollPosition(noteScrollView.scrollY)
            viewModel.saveToRecent()
        } else println("Debug: NoteEditFragment: Paused and not saved")
    }


    override fun onDestroyView() {
        clearListeners()
        super.onDestroyView()
    }

    private fun clearListeners() {
        val observer = focusObserver
        val listener = windowFocusListener
        if (observer != null && listener != null && observer.isAlive) {
            observer.removeOnWindowFocusChangeListener(listener)
        }
        focusObserver = null
        windowFocusListener = null
    }
}