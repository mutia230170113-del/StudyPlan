package com.example.studyplan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplan.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var taskAdapter: TaskAdapter

    private val allTasks = mutableListOf<Task>()
    private var currentSearchQuery = ""
    private var currentFilterStatus = "Semua Status"

    private val filterOptions = arrayOf("Semua Status", "Belum Dikerjakan", "Sedang Dikerjakan", "Selesai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        val displayName = currentUser.displayName ?: "Mahasiswa"
        binding.tvWelcome.text = getString(R.string.welcome_user, displayName)
        binding.tvProfileName.text = displayName
        binding.tvProfileEmail.text = currentUser.email

        setupRecyclerView()
        setupSearchAndFilter()
        setupBottomNavigation()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.sectionDashboard.visibility = View.VISIBLE
                    binding.sectionTasks.visibility = View.GONE
                    binding.sectionProfile.visibility = View.GONE
                    binding.fabAddTask.visibility = View.VISIBLE
                    true
                }
                R.id.nav_tasks -> {
                    binding.sectionDashboard.visibility = View.GONE
                    binding.sectionTasks.visibility = View.VISIBLE
                    binding.sectionProfile.visibility = View.GONE
                    binding.fabAddTask.visibility = View.VISIBLE
                    true
                }
                R.id.nav_profile -> {
                    binding.sectionDashboard.visibility = View.GONE
                    binding.sectionTasks.visibility = View.GONE
                    binding.sectionProfile.visibility = View.VISIBLE
                    binding.fabAddTask.visibility = View.GONE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            taskList = emptyList(),
            onItemClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java)
                intent.putExtra("EXTRA_TASK", task)
                startActivity(intent)
            },
            onEditClick = { task ->
                val intent = Intent(this, AddEditTaskActivity::class.java)
                intent.putExtra("EXTRA_TASK", task)
                startActivity(intent)
            },
            onDeleteClick = { task ->
                showDeleteConfirmation(task)
            }
        )
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter
    }

    private fun setupSearchAndFilter() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filterOptions)
        binding.spinnerFilterStatus.adapter = adapter

        binding.spinnerFilterStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilterStatus = filterOptions[position]
                applySearchAndFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty()
                applySearchAndFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                applySearchAndFilter()
                return true
            }
        })
    }

    private fun loadTasks() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                allTasks.clear()
                for (doc in documents) {
                    val task = doc.toObject(Task::class.java)
                    allTasks.add(task)
                }
                updateDashboardStats()
                applySearchAndFilter()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat tugas: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateDashboardStats() {
        val total = allTasks.size
        val done = allTasks.count { it.status == "Selesai" }
        val pending = total - done

        binding.tvTotalTasks.text = total.toString()
        binding.tvDoneTasks.text = done.toString()
        binding.tvPendingTasks.text = pending.toString()
    }

    private fun applySearchAndFilter() {
        val filtered = allTasks.filter { task ->
            val matchesSearch = task.title.contains(currentSearchQuery, ignoreCase = true) ||
                    task.course.contains(currentSearchQuery, ignoreCase = true)
            val matchesStatus = currentFilterStatus == "Semua Status" ||
                    task.status == currentFilterStatus
            matchesSearch && matchesStatus
        }

        taskAdapter.updateData(filtered)

        if (allTasks.isEmpty()) {
            binding.tvEmptyState.text = getString(R.string.empty_task)
            binding.tvEmptyState.visibility = View.VISIBLE
        } else if (filtered.isEmpty()) {
            binding.tvEmptyState.text = getString(R.string.not_found_task)
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Tugas")
            .setMessage("Yakin ingin menghapus tugas \"${task.title}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        db.collection("tasks").document(task.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadTasks()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}