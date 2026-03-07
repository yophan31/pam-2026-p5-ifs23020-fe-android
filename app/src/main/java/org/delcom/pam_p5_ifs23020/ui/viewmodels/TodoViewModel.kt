package org.delcom.pam_p5_ifs23020.ui.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.delcom.pam_p5_ifs23020.network.todos.data.RequestTodo
import org.delcom.pam_p5_ifs23020.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23020.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23020.network.todos.service.ITodoRepository
import javax.inject.Inject
import org.delcom.pam_p5_ifs23020.network.todos.data.ResponseTodoStatsData // pastikan di-import
import org.delcom.pam_p5_ifs23020.network.todos.data.RequestUserChangePassword
import org.delcom.pam_p5_ifs23020.network.todos.data.RequestUserChange

sealed interface ProfileUIState {
    data class Success(val data: ResponseUserData) : ProfileUIState
    data class Error(val message: String) : ProfileUIState
    object Loading : ProfileUIState
}

sealed interface TodosUIState {
    data class Success(val data: List<ResponseTodoData>) : TodosUIState
    data class Error(val message: String) : TodosUIState
    object Loading : TodosUIState
}

sealed interface TodoUIState {
    data class Success(val data: ResponseTodoData) : TodoUIState
    data class Error(val message: String) : TodoUIState
    object Loading : TodoUIState
}

sealed interface TodoActionUIState {
    data class Success(val message: String) : TodoActionUIState
    data class Error(val message: String) : TodoActionUIState
    object Loading : TodoActionUIState
}

sealed interface StatsUIState {
    data class Success(val data: ResponseTodoStatsData) : StatsUIState
    data class Error(val message: String) : StatsUIState
    object Loading : StatsUIState
}

data class UIStateTodo(
    val profile: ProfileUIState = ProfileUIState.Loading,
    val stats: StatsUIState = StatsUIState.Loading,
    val todos: TodosUIState = TodosUIState.Loading,
    var todo: TodoUIState = TodoUIState.Loading,
    var todoAdd: TodoActionUIState = TodoActionUIState.Loading,
    var todoChange: TodoActionUIState = TodoActionUIState.Loading,
    var todoDelete: TodoActionUIState = TodoActionUIState.Loading,
    var todoChangeCover: TodoActionUIState = TodoActionUIState.Loading,
    var profileChange: TodoActionUIState = TodoActionUIState.Loading,
    var profileChangePassword: TodoActionUIState = TodoActionUIState.Loading,
    var profileChangePhoto: TodoActionUIState = TodoActionUIState.Loading
)

@HiltViewModel
@Keep
class TodoViewModel @Inject constructor(
    private val repository: ITodoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UIStateTodo())
    val uiState = _uiState.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false
    private var currentFilter: String? = null
    private val currentTodosList = mutableListOf<ResponseTodoData>()
    private var isFetching = false
    private var currentUrgency: Int? = null

    fun getProfile(authToken: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    profile = ProfileUIState.Loading
                )
            }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.getUserMe(authToken)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") {
                            ProfileUIState.Success(it.data!!.user)
                        } else {
                            ProfileUIState.Error(it.message)
                        }
                    },
                    onFailure = {
                        ProfileUIState.Error(it.message ?: "Unknown error")
                    }
                )

                it.copy(
                    profile = tmpState
                )
            }
        }
    }

    fun resetAndGetAllTodos(
        authToken: String,
        search: String? = null,
        filter: String? = null,
        urgency: Int? = null // [BARU]
    ) {
        isFetching = false
        currentPage = 1
        isLastPage = false
        currentFilter = filter
        currentUrgency = urgency
        currentTodosList.clear()
        getAllTodos(authToken, search, currentFilter, currentUrgency)
    }

    fun getAllTodos(
        authToken: String,
        search: String? = null,
        filter: String? = currentFilter,
        urgency: Int? = currentUrgency // [BARU]
    ) {
        // Jangan request jika data sudah habis ATAU sedang ada proses mengambil data
        if (isLastPage || isFetching) return

        isFetching = true

        viewModelScope.launch {
            if (currentPage == 1) {
                _uiState.update { it.copy(todos = TodosUIState.Loading) }
            }

            _uiState.update { it ->
                val tmpState = runCatching {
                    // Pastikan repository.getTodos sudah menerima parameter urgency di posisi terakhir
                    repository.getTodos(authToken, search, currentPage, 10, filter, urgency)
                }.fold(
                    onSuccess = { response ->
                        isFetching = false

                        if (response.status == "success") {
                            val newTodos = response.data?.todos ?: emptyList()
                            if (newTodos.size < 10) isLastPage = true

                            // Filter keamanan: Jangan masukkan Todo yang ID-nya sudah ada di list (anti-duplikat)
                            val uniqueTodos = newTodos.filter { newTodo ->
                                currentTodosList.none { existingTodo -> existingTodo.id == newTodo.id }
                            }

                            currentTodosList.addAll(uniqueTodos)
                            currentPage++
                            TodosUIState.Success(currentTodosList.toList())
                        } else {
                            TodosUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        isFetching = false
                        TodosUIState.Error(error.message ?: "Unknown error")
                    }
                )

                it.copy(todos = tmpState)
            }
        }
    }

    fun getTodoStats(authToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(stats = StatsUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.getTodoStats(authToken)
                }.fold(
                    onSuccess = { response ->
                        if (response.status == "success") {
                            StatsUIState.Success(response.data!!.stats)
                        } else {
                            StatsUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        StatsUIState.Error(error.message ?: "Unknown error")
                    }
                )
                it.copy(stats = tmpState)
            }
        }
    }

    fun postTodo(
        authToken: String,
        title: String,
        description: String,
        urgency: Int // [BARU] Tambahkan parameter urgency
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todoAdd = TodoActionUIState.Loading
                )
            }
            _uiState.update { state ->
                val tmpState = runCatching {
                    repository.postTodo(
                        authToken = authToken,
                        RequestTodo(
                            title = title,
                            description = description,
                            urgency = urgency // [BARU] Masukkan ke dalam request
                        )
                    )
                }.fold(
                    onSuccess = { response ->
                        if (response.status == "success") {
                            TodoActionUIState.Success(response.message)
                        } else {
                            TodoActionUIState.Error(response.message)
                        }
                    },
                    onFailure = { error ->
                        TodoActionUIState.Error(error.message ?: "Unknown error")
                    }
                )

                state.copy(
                    todoAdd = tmpState
                )
            }
        }
    }

    fun getTodoById(authToken: String, todoId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todo = TodoUIState.Loading
                )
            }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.getTodoById(authToken, todoId)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") {
                            TodoUIState.Success(it.data!!.todo)
                        } else {
                            TodoUIState.Error(it.message)
                        }
                    },
                    onFailure = {
                        TodoUIState.Error(it.message ?: "Unknown error")
                    }
                )

                it.copy(
                    todo = tmpState
                )
            }
        }
    }

    fun putTodo(
        authToken: String,
        todoId: String,
        title: String,
        description: String,
        isDone: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todoChange = TodoActionUIState.Loading
                )
            }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putTodo(
                        authToken = authToken,
                        todoId = todoId,
                        RequestTodo(
                            title = title,
                            description = description,
                            isDone = isDone
                        )
                    )
                }.fold(
                    onSuccess = {
                        if (it.status == "success") {
                            TodoActionUIState.Success(it.message)
                        } else {
                            TodoActionUIState.Error(it.message)
                        }
                    },
                    onFailure = {
                        TodoActionUIState.Error(it.message ?: "Unknown error")
                    }
                )

                it.copy(
                    todoChange = tmpState
                )
            }
        }
    }

    fun putTodoCover(
        authToken: String,
        todoId: String,
        file: MultipartBody.Part
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todoChangeCover = TodoActionUIState.Loading
                )
            }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putTodoCover(
                        authToken = authToken,
                        todoId = todoId,
                        file = file
                    )
                }.fold(
                    onSuccess = {
                        if (it.status == "success") {
                            TodoActionUIState.Success(it.message)
                        } else {
                            TodoActionUIState.Error(it.message)
                        }
                    },
                    onFailure = {
                        TodoActionUIState.Error(it.message ?: "Unknown error")
                    }
                )

                it.copy(
                    todoChangeCover = tmpState
                )
            }
        }
    }


    fun deleteTodo(authToken: String, todoId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    todoDelete = TodoActionUIState.Loading
                )
            }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.deleteTodo(
                        authToken = authToken,
                        todoId = todoId
                    )
                }.fold(
                    onSuccess = {
                        if (it.status == "success") {
                            TodoActionUIState.Success(it.message)
                        } else {
                            TodoActionUIState.Error(it.message)
                        }
                    },
                    onFailure = {
                        TodoActionUIState.Error(it.message ?: "Unknown error")
                    }
                )

                it.copy(
                    todoDelete = tmpState
                )
            }
        }
    }

    fun putUserMe(authToken: String, name: String, username: String, about: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChange = TodoActionUIState.Loading) }
            _uiState.update { state ->
                val tmpState = runCatching {
                    // Mengirimkan objek request yang sudah mendukung field 'about'
                    repository.putUserMe(authToken, RequestUserChange(name, username, about))
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                state.copy(profileChange = tmpState)
            }
        }
    }

    fun putUserMePassword(authToken: String, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChangePassword = TodoActionUIState.Loading) }
            _uiState.update { state ->
                val tmpState = runCatching {
                    repository.putUserMePassword(authToken, RequestUserChangePassword(oldPassword, newPassword))
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                state.copy(profileChangePassword = tmpState)
            }
        }
    }

    fun putUserMePhoto(authToken: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChangePhoto = TodoActionUIState.Loading) }
            _uiState.update { state ->
                val tmpState = runCatching {
                    repository.putUserMePhoto(authToken, file)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                state.copy(profileChangePhoto = tmpState)
            }
        }
    }
}