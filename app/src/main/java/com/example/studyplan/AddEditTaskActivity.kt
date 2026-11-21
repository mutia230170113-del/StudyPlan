package com.example.studyplan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studyplan.databinding.ActivityAddEditTaskBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var existingTask: Task? = null

    private val priorities = arrayOf("Rendah", "Sedang", "Tinggi")
    private val statuses = arrayOf("Belum Dikerjakan", "Sedang Dikerjakan", "Selesai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupSpinners()
        setupDatePicker()

        // Cek apakah ini mode Edit (membawa data tugas)
        @Suppress("DEPRECATION")
        existingTask = intent.getSerializableExtra("EXTRA_TASK") as? Task

        if (existingTask != null) {
            binding.tvPageTitle.text = getString(R.string.title_edit_task)
            binding.etTitle.setText(existingTask?.title)
            binding.etCourse.setText(existingTask?.course)
            binding.etDescription.setText(existingTask?.description)
            binding.etDeadline.setText(existingTask?.deadline)

            val priorityIndex = priorities.indexOf(existingTask?.priority)
            if (priorityIndex >= 0) binding.spinnerPriority.setSelection(priorityIndex)

            val statusIndex = statuses.indexOf(existingTask?.status)
            if (statusIndex >= 0) binding.spinnerStatus.setSelection(statusIndex)
        }

        binding.btnSaveTask.setOnClickListener {
            saveTask()
        }
    }

    private fun setupSpinners() {
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        binding.spinnerPriority.adapter = priorityAdapter

        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        binding.spinnerStatus.adapter = statusAdapter
    }

    private fun setupDatePicker() {
        binding.etDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%02d-%02d-%04d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
                )
                binding.etDeadline.setText(formattedDate)
            }, year, month, day).show()
        }
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val deadline = binding.etDeadline.text.toString().trim()
        val priority = binding.spinnerPriority.selectedItem.toString()
        val status = binding.spinnerStatus.selectedItem.toString()

        binding.tilTitle.error = null
        binding.tilCourse.error = null
        binding.tilDeadline.error = null

        if (title.isEmpty()) {
            binding.tilTitle.error = "Judul tugas tidak boleh kosong"
            return
        }
        if (course.isEmpty()) {
            binding.tilCourse.error = "Mata kuliah tidak boleh kosong"
            return
        }
        if (deadline.isEmpty()) {
            binding.tilDeadline.error = "Deadline harus dipilih"
            return
        }

        val userId = auth.currentUser?.uid ?: return
        setLoading(true)

        if (existingTask == null) {
            // Mode CREATE (Tambah Baru)
            val docRef = db.collection("tasks").document()
            val newTask = Task(
                id = docRef.id,
                userId = userId,
                title = title,
                course = course,
                description = description,
                deadline = deadline,
                priority = priority,
                status = status
            )
            docRef.set(newTask)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(this, "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal menyimpan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Mode UPDATE (Edit)
            val updatedTask = Task(
                id = existingTask!!.id,
                userId = userId,
                title = title,
                course = course,
                description = description,
                deadline = deadline,
                priority = priority,
                status = status
            )
            db.collection("tasks").document(existingTask!!.id).set(updatedTask)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(this, "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal memperbarui: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveTask.isEnabled = !isLoading
    }
}