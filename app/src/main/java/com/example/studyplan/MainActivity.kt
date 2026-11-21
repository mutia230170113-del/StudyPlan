package com.example.studyplan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplan.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()

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

        setupRecyclerView()

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

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            taskList = taskList,
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

    private fun loadTasks() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                taskList.clear()
                for (doc in documents) {
                    val task = doc.toObject(Task::class.java)
                    taskList.add(task)
                }
                taskAdapter.updateData(taskList)

                if (taskList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat tugas: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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