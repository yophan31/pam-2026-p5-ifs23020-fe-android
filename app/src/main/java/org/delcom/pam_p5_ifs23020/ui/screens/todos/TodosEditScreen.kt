package org.delcom.pam_p5_ifs23020.ui.screens.todos

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23020.R
import org.delcom.pam_p5_ifs23020.helper.ConstHelper
import org.delcom.pam_p5_ifs23020.helper.RouteHelper
import org.delcom.pam_p5_ifs23020.helper.ToolsHelper
import org.delcom.pam_p5_ifs23020.helper.ToolsHelper.uriToMultipart
import org.delcom.pam_p5_ifs23020.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23020.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23020.ui.viewmodels.*

@Composable
fun TodosEditScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel,
    todoId: String
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var authToken by remember { mutableStateOf("") }

    // Form fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isDone by remember { mutableStateOf(false) }
    var urgency by remember { mutableStateOf(1) }

    // Cover preview URI (lokal, null = pakai cover dari server)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    // Launcher pilih gambar cover
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val filePart = uriToMultipart(context, it, "cover")
            todoViewModel.putTodoCover(authToken, todoId, filePart)
        }
    }

    // Ambil auth token & load todo
    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
            return@LaunchedEffect
        }
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        todoViewModel.getTodoById(authToken, todoId)
    }

    // Isi form saat data todo berhasil dimuat
    LaunchedEffect(uiStateTodo.todo) {
        when (val state = uiStateTodo.todo) {
            is TodoUIState.Loading -> isLoading = true
            is TodoUIState.Success -> {
                isLoading = false
                val todo = state.data
                title = todo.title
                description = todo.description
                isDone = todo.isDone
                urgency = todo.urgency
            }
            is TodoUIState.Error -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                RouteHelper.back(navController)
            }
        }
    }

    // Pantau hasil update todo
    LaunchedEffect(uiStateTodo.todoChange) {
        when (val state = uiStateTodo.todoChange) {
            is TodoActionUIState.Success -> {
                Toast.makeText(context, "Todo berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                todoViewModel.uiState.value.todoChange = TodoActionUIState.Loading
                RouteHelper.back(navController)
            }
            is TodoActionUIState.Error -> {
                isLoading = false
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                todoViewModel.uiState.value.todoChange = TodoActionUIState.Loading
            }
            else -> {}
        }
    }

    // Pantau hasil update cover
    LaunchedEffect(uiStateTodo.todoChangeCover) {
        when (val state = uiStateTodo.todoChangeCover) {
            is TodoActionUIState.Success -> {
                Toast.makeText(context, "Cover berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                todoViewModel.uiState.value.todoChangeCover = TodoActionUIState.Loading
            }
            is TodoActionUIState.Error -> {
                Toast.makeText(context, "Gagal update cover: ${state.message}", Toast.LENGTH_SHORT).show()
                todoViewModel.uiState.value.todoChangeCover = TodoActionUIState.Loading
            }
            else -> {}
        }
    }

    if (isLoading) {
        LoadingUI()
        return
    }

    val t = System.currentTimeMillis().toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Edit Todo",
            showBackButton = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover Preview + Tombol Ganti
            val coverModel: Any = selectedImageUri
                ?: ToolsHelper.getTodoImage(todoId, t)

            AsyncImage(
                model = coverModel,
                contentDescription = "Cover Todo",
                placeholder = painterResource(R.drawable.img_placeholder),
                error = painterResource(R.drawable.img_placeholder),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Ganti Cover")
            }

            HorizontalDivider()

            // Field Judul
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Field Deskripsi
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                minLines = 3
            )

            // Pilih Urgensi
            Text(
                text = "Tingkat Urgensi",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Rendah", 2 to "Sedang", 3 to "Tinggi").forEach { (value, label) ->
                    val isSelected = urgency == value
                    val chipColor = when (value) {
                        3 -> Color(0xFFF44336)
                        2 -> Color(0xFFFFA726)
                        else -> Color(0xFF42A5F5)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { urgency = value },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                            selectedLabelColor = chipColor
                        )
                    )
                }
            }

            // Toggle isDone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tandai Selesai",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = isDone,
                    onCheckedChange = { isDone = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Simpan
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    todoViewModel.putTodo(authToken, todoId, title, description, isDone)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Perubahan", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}