package com.example.studyplan

import java.io.Serializable

data class Task(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var course: String = "",
    var description: String = "",
    var deadline: String = "",
    var priority: String = "Sedang",
    var status: String = "Belum Dikerjakan"
) : Serializable