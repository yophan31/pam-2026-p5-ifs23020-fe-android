package org.delcom.pam_p5_ifs23020.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.delcom.pam_p5_ifs23020.helper.AlertHelper
import org.delcom.pam_p5_ifs23020.helper.AlertState
import org.delcom.pam_p5_ifs23020.helper.AlertType
import org.delcom.pam_p5_ifs23020.helper.ConstHelper
import org.delcom.pam_p5_ifs23020.helper.RouteHelper
import org.delcom.pam_p5_ifs23020.helper.SuspendHelper
import org.delcom.pam_p5_ifs23020.helper.SuspendHelper.SnackBarType
import org.delcom.pam_p5_ifs23020.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23020.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23020.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23020.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23020.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23020.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23020.ui.viewmodels.TodoActionUIState
import org.delcom.pam_p5_ifs23020.ui.viewmodels.TodoViewModel
import androidx.compose.runtime.mutableIntStateOf // Import khusus untuk angka
import androidx.compose.material3.RadioButton  // Pastikan RadioButton juga di-import
import androidx.compose.foundation.layout.Row // Untuk Row
import androidx.compose.foundation.clickable      // Untuk clickable

@Composable
fun TodosAddScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    // Ambil data dari viewmodel
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var tmpTodo by remember { mutableStateOf<ResponseTodoData?>(null) }
    val authToken = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Reset status todo action

        if(uiStateAuth.auth !is AuthUIState.Success){
            RouteHelper.to(
                navController,
                ConstHelper.RouteNames.Home.path,
                true
            )
            return@LaunchedEffect
        }

        authToken.value = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        uiStateTodo.todoAdd = TodoActionUIState.Loading
    }

    // Simpan data
    fun onSave(
        title: String,
        description: String,
        urgency: Int // [TAMBAHKAN INI]
    ) {
        if(authToken.value == null){
            return
        }

        isLoading = true

        tmpTodo = ResponseTodoData(
            title = title,
            description = description,
            urgency = urgency // [TAMBAHKAN INI]
        )

        todoViewModel.postTodo(
            authToken = authToken.value!!,
            title = title,
            description = description,
            urgency = urgency // [TAMBAHKAN INI]
        )
    }

    LaunchedEffect(uiStateTodo.todoAdd) {
        when (val state = uiStateTodo.todoAdd) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(
                    snackbarHost = snackbarHost,
                    type = SnackBarType.SUCCESS,
                    message = state.message
                )
                RouteHelper.to(
                    navController,
                    ConstHelper.RouteNames.Todos.path,
                    true
                )
                isLoading = false
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(
                    snackbarHost = snackbarHost,
                    type = SnackBarType.ERROR,
                    message = state.message
                )
                isLoading = false
            }
            else -> {}
        }
    }

    // Tampilkan halaman loading
    if (isLoading) {
        LoadingUI()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBarComponent(
            navController = navController,
            title = "Tambah Todo",
            showBackButton = true,
        )
        // Content
        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            TodosAddUI(
                tmpTodo = tmpTodo,
                onSave = ::onSave
            )
        }
        // Bottom Nav
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosAddUI(
    tmpTodo: ResponseTodoData?,
    onSave: (
        String, // Title
        String, // Description
        Int     // Urgency [BARU]
    ) -> Unit
) {
    val alertState = remember { mutableStateOf(AlertState()) }

    var dataTitle by remember { mutableStateOf(tmpTodo?.title ?: "") }
    var dataDescription by remember { mutableStateOf(tmpTodo?.description ?: "") }
    // [BARU] Inisialisasi state urgensi (default 1 = Low)
    var dataUrgency by remember { mutableIntStateOf(tmpTodo?.urgency ?: 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        OutlinedTextField(
            value = dataTitle,
            onValueChange = { dataTitle = it },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            label = { Text(text = "Title") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
        )

        // Description
        OutlinedTextField(
            value = dataDescription,
            onValueChange = { dataDescription = it },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            label = { Text(text = "Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            maxLines = 5,
            minLines = 3
        )

        // [BARU] Pilihan Urgensi
        Text(
            text = "Tingkat Urgensi",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val urgencies = listOf("Low" to 1, "Medium" to 2, "High" to 3)
            urgencies.forEach { (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { dataUrgency = value }
                ) {
                    RadioButton(
                        selected = (dataUrgency == value),
                        onClick = { dataUrgency = value }
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = {
                if(dataTitle.isEmpty()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Judul tidak boleh kosong!")
                    return@FloatingActionButton
                }

                if(dataDescription.isEmpty()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Deskripsi tidak boleh kosong!")
                    return@FloatingActionButton
                }

                // Kirim 3 data ke fungsi onSave
                onSave(
                    dataTitle,
                    dataDescription,
                    dataUrgency
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan Data")
        }
    }

    // Alert Dialog tetap sama
    if (alertState.value.isVisible) {
        AlertDialog(
            onDismissRequest = { AlertHelper.dismiss(alertState) },
            title = { Text(alertState.value.type.title) },
            text = { Text(alertState.value.message) },
            confirmButton = {
                TextButton(onClick = { AlertHelper.dismiss(alertState) }) {
                    Text("OK")
                }
            }
        )
    }
}


@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewTodosAddUI() {
//    DelcomTheme {
//        TodosAddUI(
//            todos = DummyData.getTodosAddData(),
//            onOpen = {}
//        )
//    }
}