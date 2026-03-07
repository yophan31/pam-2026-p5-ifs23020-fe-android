package org.delcom.pam_p5_ifs23020.network.todos.data

import kotlinx.serialization.Serializable

@Serializable
data class ResponseTodos (
    val todos: List<ResponseTodoData>
)

@Serializable
data class ResponseTodo (
    val todo: ResponseTodoData
)

@Serializable
data class ResponseTodoData(
    val id: String = "",
    val userId: String = "",
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val cover: String? = null,
    val urgency: Int = 1,
    val createdAt: String = "",
    var updatedAt: String = ""
)

@Serializable
data class ResponseTodoAdd (
    val todoId: String
)

@Serializable
data class ResponseTodoStats (
    val stats: ResponseTodoStatsData
)

@Serializable
data class ResponseTodoStatsData(
    val total: Long = 0,
    val complete: Long = 0,
    val active: Long = 0
)