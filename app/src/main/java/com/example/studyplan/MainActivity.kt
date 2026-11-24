package com.example.studyplan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplan.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var upcomingAdapter: TaskAdapter

    private val allTasks = mutableListOf<Task>()
    private var currentSearchQuery = ""
    private var currentFilterStatus = "Semua Status"

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Izin notifikasi ditolak. Pengingat tidak akan muncul.", Toast.LENGTH_SHORT).show()
            }
        }

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

        updateUserProfileUI(currentUser.displayName ?: "Mahasiswa", currentUser.email ?: "")

        setupRecyclerViews()
        setupSearchAndFilter()
        setupBottomNavigation()
        setupProfileMenu()
        askNotificationPermission()

        binding.btnRetry.setOnClickListener { loadTasks() }

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

    private fun updateUserProfileUI(name: String, email: String) {
        binding.tvWelcome.text = getString(R.string.welcome_user, name)
        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        binding.tvProfileInitials.text = getInitials(name)
    }

    private fun getInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].first().uppercaseChar().toString()
            else -> "M"
        }
    }

    private fun setupProfileMenu() {
        // 1. Edit Nama Profil
        binding.menuEditName.setOnClickListener {
            val input = EditText(this).apply {
                setText(auth.currentUser?.displayName ?: "")
                hint = "Masukkan nama baru"
                setPadding(48, 32, 48, 32)
            }
            AlertDialog.Builder(this)
                .setTitle("Edit Nama Profil")
                .setView(input)
                .setPositiveButton("Simpan") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        val request = UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build()
                        auth.currentUser?.updateProfile(request)?.addOnSuccessListener {
                            updateUserProfileUI(newName, auth.currentUser?.email ?: "")
                            Toast.makeText(this, "Nama berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // 2. Toggle Notifikasi
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                askNotificationPermission()
                Toast.makeText(this, "Pengingat notifikasi aktif", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Pengingat notifikasi dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Tentang Aplikasi
        binding.menuAboutApp.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tentang StudyPlan")
                .setMessage("StudyPlan v1.0\n\nAplikasi manajemen tugas kuliah berbasis Android dengan Firebase Authentication, Cloud Firestore, dan Pengingat Deadline.")
                .setPositiveButton("Tutup", null)
                .show()
        }
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

    private fun setupRecyclerViews() {
        val onItemClick: (Task) -> Unit = { task ->
            val intent = Intent(this, TaskDetailActivity::class.java)
            intent.putExtra("EXTRA_TASK", task)
            startActivity(intent)
        }
        val onEditClick: (Task) -> Unit = { task ->
            val intent = Intent(this, AddEditTaskActivity::class.java)
            intent.putExtra("EXTRA_TASK", task)
            startActivity(intent)
        }
        val onDeleteClick: (Task) -> Unit = { task ->
            showDeleteConfirmation(task)
        }

        taskAdapter = TaskAdapter(emptyList(), onItemClick, onEditClick, onDeleteClick)
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter

        upcomingAdapter = TaskAdapter(emptyList(), onItemClick, onEditClick, onDeleteClick)
        binding.rvUpcomingTasks.layoutManager = LinearLayoutManager(this)
        binding.rvUpcomingTasks.adapter = upcomingAdapter
    }

    private fun setupSearchAndFilter() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterStatus = when (checkedIds.firstOrNull()) {
                R.id.chipPending -> "Belum Dikerjakan"
                R.id.chipInProgress -> "Sedang Dikerjakan"
                R.id.chipDone -> "Selesai"
                else -> "Semua Status"
            }
            applySearchAndFilter()
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
        binding.layoutErrorState.visibility = View.GONE

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
                binding.layoutErrorState.visibility = View.VISIBLE
                Toast.makeText(this, "Gagal memuat tugas: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateDashboardStats() {
        val total = allTasks.size
        val done = allTasks.count { it.status == "Selesai" }
        val pending = total - done
        val progressPercent = if (total > 0) (done * 100) / total else 0

        binding.tvTotalTasks.text = total.toString()
        binding.tvDoneTasks.text = done.toString()
        binding.tvPendingTasks.text = pending.toString()

        // Update Progress Bar Beranda & Produktivitas Profil
        binding.tvProgressPercent.text = "$progressPercent%"
        binding.progressBarTasks.progress = progressPercent
        binding.tvProfileDoneCount.text = done.toString()
        binding.tvProfileProgressPercent.text = "$progressPercent%"

        // Tampilkan maks 3 tugas belum selesai di Beranda
        val upcoming = allTasks.filter { it.status != "Selesai" }.take(3)
        upcomingAdapter.updateData(upcoming)
        binding.tvUpcomingEmpty.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
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
            .setPositiveButton("Hapus") { _, _ -> deleteTask(task) }
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

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}