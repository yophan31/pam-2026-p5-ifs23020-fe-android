package org.delcom.pam_p5_ifs23020.ui.screens.todos

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import org.delcom.pam_p5_ifs23020.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23020.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23020.ui.viewmodels.*

@Composable
fun TodosDetailScreen(
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
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Ambil auth token & load todo
    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
            return@LaunchedEffect
        }
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        todoViewModel.getTodoById(authToken, todoId)
    }

    // Pantau state todo
    LaunchedEffect(uiStateTodo.todo) {
        when (uiStateTodo.todo) {
            is TodoUIState.Loading -> isLoading = true
            is TodoUIState.Success -> isLoading = false
            is TodoUIState.Error -> {
                isLoading = false
                Toast.makeText(context, (uiStateTodo.todo as TodoUIState.Error).message, Toast.LENGTH_SHORT).show()
                RouteHelper.back(navController)
            }
        }
    }

    // Pantau state delete
    LaunchedEffect(uiStateTodo.todoDelete) {
        when (val state = uiStateTodo.todoDelete) {
            is TodoActionUIState.Success -> {
                Toast.makeText(context, "Todo berhasil dihapus!", Toast.LENGTH_SHORT).show()
                todoViewModel.uiState.value.todoDelete = TodoActionUIState.Loading
                RouteHelper.back(navController)
            }
            is TodoActionUIState.Error -> {
                isLoading = false
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                todoViewModel.uiState.value.todoDelete = TodoActionUIState.Loading
            }
            else -> {}
        }
    }

    if (isLoading || uiStateTodo.todo !is TodoUIState.Success) {
        LoadingUI()
        return
    }

    val todo = (uiStateTodo.todo as TodoUIState.Success).data
    val t = System.currentTimeMillis().toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Detail Todo",
            showBackButton = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover Gambar
            AsyncImage(
                model = ToolsHelper.getTodoImage(todo.id, t),
                contentDescription = "Cover Todo",
                placeholder = painterResource(R.drawable.img_placeholder),
                error = painterResource(R.drawable.img_placeholder),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            // Badge Status & Urgency
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Status isDone
                val statusColor = if (todo.isDone) Color(0xFF4CAF50) else Color(0xFFFFA726)
                val statusText = if (todo.isDone) "Selesai" else "Aktif"
                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Urgency Badge
                val urgencyColor = when (todo.urgency) {
                    3 -> Color(0xFFF44336)
                    2 -> Color(0xFFFFA726)
                    else -> Color(0xFF42A5F5)
                }
                val urgencyText = when (todo.urgency) {
                    3 -> "Urgensi Tinggi"
                    2 -> "Urgensi Sedang"
                    else -> "Urgensi Rendah"
                }
                Surface(shape = RoundedCornerShape(50), color = urgencyColor.copy(alpha = 0.15f)) {
                    Text(
                        text = urgencyText,
                        color = urgencyColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Judul
            Text(
                text = todo.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Deskripsi
            if (todo.description.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Deskripsi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todo.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Aksi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        navController.navigate(
                            ConstHelper.RouteNames.TodosEdit.path.replace("{todoId}", todo.id)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus")
                }
            }
        }
    }

    // Dialog Konfirmasi Hapus
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Todo") },
            text = { Text("Apakah kamu yakin ingin menghapus \"${todo.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        isLoading = true
                        todoViewModel.deleteTodo(authToken, todo.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
}