package com.example.whiteleafnotes.presentation.root

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whiteleafnotes.R
import com.example.whiteleafnotes.common.utils.DialogHelper
import com.example.whiteleafnotes.databinding.ActivityRootBinding
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.presentation.note_list.NoteListFragmentDirections
import org.koin.androidx.viewmodel.ext.android.viewModel

class RootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRootBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerMenuAdapter: DrawerMenuAdapter
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
        setupDrawerMenu()
        setupObservers()
        setupNavigationListener()
    }

    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.startFragment -> supportActionBar?.hide()

                R.id.noteListFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.subtitle = "Записная книжка"
                }

                R.id.settingsFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.subtitle = null
                }

                R.id.noteEditFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = ""
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
        drawerLayout = binding.drawerLayout

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) =
                menuViewModel.loadMenuData()

            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.noteListFragment),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupDrawerMenu() {
        drawerMenuAdapter = DrawerMenuAdapter(
            onNotebookClicked = { notebook ->
                navigateToNotebook(notebook)
                drawerLayout.closeDrawer(GravityCompat.START)
            },
            onNoteClicked = { note ->
                navigateToNote(note)
                drawerLayout.closeDrawer(GravityCompat.START)
            },
            onCreateNotebook = {
                DialogHelper.createCreateNotebookDialog(this) { name ->
                    menuViewModel.createNewNotebook(name)
                }.show()
            },
            onCreateNote = {
                menuViewModel.createNewNote()
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        )
        binding.navView.getHeaderView(0).findViewById<RecyclerView>(R.id.drawer_menu_recyclerView)
            .apply {
                adapter = drawerMenuAdapter
                layoutManager = LinearLayoutManager(this@RootActivity)
            }

        binding.navView.getHeaderView(0).findViewById<TextView>(R.id.app_title).setOnClickListener {
            val action = NoteListFragmentDirections.actionGlobalStartFragment()
            navController.navigate(action)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        menuViewModel.loadMenuData()
    }

    private fun setupObservers() {
        menuViewModel.menuItems.observe(this) { items ->
            drawerMenuAdapter.submitList(items)
        }

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
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = NoteListFragmentDirections.actionGlobalNoteListFragment(notebook.path)
        navController.navigate(action)
    }

    private fun navigateToNote(note: Note) {
        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditFragment(
            noteId = note.id,
            notebookPath = null
        )
        navController.navigate(action)
    }

    fun navigateToCreatedNote(note: Note) {
        val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
            noteId = note.id,
            notebookPath = null
        )
        navController.navigate(action)
    }

    override fun onSupportNavigateUp(): Boolean {
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