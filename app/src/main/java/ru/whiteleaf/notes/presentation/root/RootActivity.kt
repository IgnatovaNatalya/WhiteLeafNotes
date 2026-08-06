package ru.whiteleaf.notes.presentation.root


import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.databinding.ActivityRootBinding
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.presentation.note_list.NoteListFragmentDirections
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.whiteleaf.notes.common.utils.DialogHelper.createCreateNotebookDialog
import ru.whiteleaf.notes.presentation.note_edit.NoteEditFragment

class RootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRootBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var appBarConfiguration: AppBarConfiguration

    private val menuViewModel: DrawerMenuViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeApp()
    }

    private fun initializeApp() {
        enableEdgeToEdge()
        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
        setupToolbar()
        setupNavigation()
        setupObservers()
        setupNavigationListener()
    }

    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val startHeader = findViewById<TextView>(R.id.tv_toolbar_title)
            val lockIndicatorButton = findViewById<ImageButton>(R.id.btn_lock_indicator)
            val optionsButton = findViewById<ImageButton>(R.id.btn_options_menu)
            val searchButton = findViewById<ImageButton>(R.id.btn_search)

            when (destination.id) {
                R.id.startFragment -> {
                    startHeader.visibility = View.VISIBLE
                    lockIndicatorButton.visibility = View.GONE
                    optionsButton.visibility = View.GONE
                    searchButton.visibility = View.VISIBLE
                    supportActionBar?.title = ""
                    supportActionBar?.subtitle = null
                }

                R.id.noteListFragment -> {
                    startHeader.visibility = View.GONE
                    lockIndicatorButton.visibility = View.VISIBLE
                    optionsButton.visibility = View.VISIBLE
                    searchButton.visibility = View.GONE
                    supportActionBar?.subtitle = "Записная книжка"
                }

                R.id.notebooksFragment -> {
                    startHeader.visibility = View.GONE
                    lockIndicatorButton.visibility = View.GONE
                    optionsButton.visibility = View.VISIBLE
                    searchButton.visibility = View.VISIBLE
                    supportActionBar?.subtitle = null
                }

                R.id.settingsFragment -> {
                    startHeader.visibility = View.GONE
                    lockIndicatorButton.visibility = View.GONE
                    optionsButton.visibility = View.GONE
                    searchButton.visibility = View.GONE
                    supportActionBar?.subtitle = null
                }

                R.id.noteEditFragment -> {
                    startHeader.visibility = View.GONE
                    lockIndicatorButton.visibility = View.VISIBLE
                    optionsButton.visibility = View.VISIBLE
                    searchButton.visibility = View.GONE
                    supportActionBar?.subtitle = null
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        val navView = binding.navView

        val menu = navView.menu

        customizeMenuItem(menu.findItem(R.id.menu_create_note))
        customizeMenuItem(menu.findItem(R.id.menu_create_notebook))

        NavigationUI.setupWithNavController(navView, navController)

        drawerLayout = binding.drawerLayout

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.startFragment,
                R.id.settingsFragment,
                R.id.notebooksFragment
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_create_note -> {
                    menuViewModel.createNewNote()
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.menu_create_notebook -> {
                    createCreateNotebookDialog(this) { name ->
                        menuViewModel.createNewNotebook(name)
                    }.show()
                    drawerLayout.closeDrawers()
                    true
                }

                else -> {
                    drawerLayout.closeDrawers()
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                }
            }
        }
    }

    private fun setupObservers() {
        menuViewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                menuViewModel.clearError()
            }
        }

        menuViewModel.isLoading.observe(this) { isLoading ->
            // todo индикатор загрузки
            // if (isLoading) { }
        }

        menuViewModel.navigateToCreatedNote.observe(this) { note ->
            note?.let {
                navigateToCreatedNote(note)
                menuViewModel.onNoteNavigated()
            }
        }
        menuViewModel.navigateToCreatedNotebook.observe(this) { notebook ->
            notebook?.let {
                navigateToNotebook(notebook)
                menuViewModel.onNotebookNavigated()
            }
        }
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = NoteListFragmentDirections.actionGlobalNoteListFragment(notebook.path)
        navController.navigate(action)
    }

    fun navigateToCreatedNote(note: Note) {
        val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
            noteId = note.id,
            notebookPath = null
        )
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.startFragment, false)
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
        navController.navigate(action, navOptions)
    }

    fun customizeMenuItem(menuItem: MenuItem, textSizeSp: Int = 16) {
        val title = menuItem.title.toString()
        val spannable = SpannableString(title)

        spannable.setSpan(
            AbsoluteSizeSpan(textSizeSp, true), // true = размер в sp
            0,
            title.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            title.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        menuItem.title = spannable
    }

    override fun onSupportNavigateUp(): Boolean {

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is NoteEditFragment) {
            currentFragment.performSaveAndExit()
            return true
        }

        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @Deprecated("This method has been deprecated in favor of using the\n{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}