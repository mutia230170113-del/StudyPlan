package com.example.studyplan

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplan.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class TaskAdapter(
    private var taskList: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.binding.apply {
            tvTaskTitle.text = task.title
            tvTaskCourse.text = "Mata Kuliah: ${task.course}"
            tvTaskDeadline.text = "Deadline: ${task.deadline}"
            tvTaskPriority.text = task.priority
            tvTaskStatus.text = task.status

            // Hitung Mundur Deadline (Countdown)
            if (task.status == "Selesai") {
                tvDeadlineCountdown.visibility = View.GONE
            } else {
                val countdown = getDeadlineCountdown(task.deadline)
                if (countdown != null) {
                    tvDeadlineCountdown.text = countdown.first
                    tvDeadlineCountdown.backgroundTintList = ColorStateList.valueOf(Color.parseColor(countdown.second))
                    tvDeadlineCountdown.visibility = View.VISIBLE
                } else {
                    tvDeadlineCountdown.visibility = View.GONE
                }
            }

            // Warna Garis Aksen Kiri & Badge Prioritas
            val priorityColor = when (task.priority) {
                "Tinggi" -> "#D32F2F"
                "Sedang" -> "#F57C00"
                else -> "#388E3C"
            }
            val parsedPriorityColor = Color.parseColor(priorityColor)
            viewPriorityStrip.setBackgroundColor(parsedPriorityColor)
            tvTaskPriority.backgroundTintList = ColorStateList.valueOf(parsedPriorityColor)

            // Warna Badge Status
            val statusColor = when (task.status) {
                "Selesai" -> "#2E7D32"
                "Sedang Dikerjakan" -> "#1565C0"
                else -> "#64748B"
            }
            tvTaskStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(statusColor))

            root.setOnClickListener { onItemClick(task) }
            btnEdit.setOnClickListener { onEditClick(task) }
            btnDelete.setOnClickListener { onDeleteClick(task) }
        }
    }

    private fun getDeadlineCountdown(deadlineStr: String): Pair<String, String>? {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val deadlineDate = sdf.parse(deadlineStr) ?: return null

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val target = Calendar.getInstance().apply {
                time = deadlineDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = target.timeInMillis - today.timeInMillis
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            when {
                diffDays < 0 -> Pair("Terlewat", "#D32F2F")
                diffDays == 0L -> Pair("Hari Ini!", "#D32F2F")
                diffDays == 1L -> Pair("Besok!", "#F57C00")
                diffDays in 2..3 -> Pair("H-$diffDays", "#F57C00")
                else -> Pair("H-$diffDays", "#4A7FD5")
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newList: List<Task>) {
        taskList = newList
        notifyDataSetChanged()
    }
}