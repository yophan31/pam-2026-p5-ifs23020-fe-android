package org.delcom.pam_p5_ifs23020.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.delcom.pam_p5_ifs23020.R
import org.delcom.pam_p5_ifs23020.helper.ConstHelper
import org.delcom.pam_p5_ifs23020.helper.RouteHelper
import org.delcom.pam_p5_ifs23020.helper.ToolsHelper
import org.delcom.pam_p5_ifs23020.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23020.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23020.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23020.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23020.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23020.ui.theme.DelcomTheme
import org.delcom.pam_p5_ifs23020.ui.viewmodels.*
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.text.style.TextAlign


@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    // Ambil data dari viewmodel
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<ResponseUserData?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // State untuk memunculkan Dialog Edit
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditPasswordDialog by remember { mutableStateOf(false) }

    // Launcher untuk memilih gambar dari Galeri Android
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val filePart = getMultipartFromUri(context, it)
            if (filePart != null && authToken != null) {
                todoViewModel.putUserMePhoto(authToken!!, filePart)
            } else {
                Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true

        if(uiStateAuth.auth !is AuthUIState.Success){
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }

        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken

        if(uiStateTodo.profile is ProfileUIState.Success){
            profile = (uiStateTodo.profile as ProfileUIState.Success).data
            isLoading = false
            return@LaunchedEffect
        }

        todoViewModel.getProfile(authToken ?: "")
    }

    LaunchedEffect(uiStateTodo.profile) {
        if(uiStateTodo.profile !is ProfileUIState.Loading){
            isLoading = false
            if(uiStateTodo.profile is ProfileUIState.Success){
                profile = (uiStateTodo.profile as ProfileUIState.Success).data
            }else{
                RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            }
        }
    }

    // Memantau aksi Update Profile (Nama/Username, Foto, Sandi)
    LaunchedEffect(uiStateTodo.profileChange, uiStateTodo.profileChangePassword, uiStateTodo.profileChangePhoto) {
        val states = listOf(uiStateTodo.profileChange, uiStateTodo.profileChangePassword, uiStateTodo.profileChangePhoto)

        states.forEach { state ->
            if (state is TodoActionUIState.Success) {
                Toast.makeText(context, "Berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                // Reset state & muat ulang data terbaru agar layar ter-refresh
                todoViewModel.uiState.value.profileChange = TodoActionUIState.Loading
                todoViewModel.uiState.value.profileChangePassword = TodoActionUIState.Loading
                todoViewModel.uiState.value.profileChangePhoto = TodoActionUIState.Loading
                todoViewModel.getProfile(authToken ?: "")
            } else if (state is TodoActionUIState.Error) {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onLogout(token: String){
        isLoading = true
        authViewModel.logout(token)
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    // Tampilkan halaman loading
    if(isLoading || profile == null){
        LoadingUI()
        return
    }

    // Menu Top App Bar
    val menuItems = listOf(
        TopAppBarMenuItem(
            text = "Logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            route = null,
            onClick = { onLogout(authToken ?: "") }
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBarComponent(
            navController = navController,
            title = "Profile",
            showBackButton = false,
            customMenuItems = menuItems
        )

        // Content
        Box(modifier = Modifier.weight(1f)) {
            ProfileUI(
                profile = profile!!,
                onEditPhotoClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onEditProfileClick = { showEditProfileDialog = true },
                onEditPasswordClick = { showEditPasswordDialog = true }
            )
        }
        // Bottom Nav
        BottomNavComponent(navController = navController)
    }

    // ==========================================
    // DIALOG EDIT PROFIL
    // ==========================================
    if (showEditProfileDialog) {
        var inputName by remember { mutableStateOf(profile!!.name) }
        var inputUsername by remember { mutableStateOf(profile!!.username) }
        var inputAbout by remember { mutableStateOf(profile!!.about ?: "") } // Ambil dari data profile

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputAbout,
                        onValueChange = { inputAbout = it },
                        label = { Text("Tentang Saya") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    todoViewModel.putUserMe(authToken ?: "", inputName, inputUsername,inputAbout)
                    showEditProfileDialog = false
                    isLoading = true
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Batal") }
            }
        )
    }

    // ==========================================
    // DIALOG UBAH KATA SANDI
    // ==========================================
    if (showEditPasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditPasswordDialog = false },
            title = { Text("Ubah Kata Sandi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Sandi Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Sandi Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    todoViewModel.putUserMePassword(authToken ?: "", oldPassword, newPassword)
                    showEditPasswordDialog = false
                    isLoading = true
                }) { Text("Ubah Sandi") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPasswordDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun ProfileUI(
    profile: ResponseUserData,
    onEditPhotoClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEditPasswordClick: () -> Unit
){
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Header Profile
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Foto Profil dengan Ikon Edit
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        // Tambahkan param ?time= agar coil merefresh cache gambar saat diupdate
                        model = ToolsHelper.getUserImage(profile.id) + "?time=${System.currentTimeMillis()}",
                        contentDescription = "Photo Profil",
                        placeholder = painterResource(R.drawable.img_placeholder),
                        error = painterResource(R.drawable.img_placeholder),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { onEditPhotoClick() }
                    )
                    // Ikon Pensil
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onEditPhotoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Foto",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = profile.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "@${profile.username}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!profile.about.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.about!!,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Aksi
        Button(
            onClick = onEditProfileClick,
            modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profil")
        }

        OutlinedButton(
            onClick = onEditPasswordClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ubah Kata Sandi")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==========================================
// FUNGSI BANTUAN UNTUK UPLOAD GAMBAR
// ==========================================
fun getMultipartFromUri(context: Context, uri: Uri): MultipartBody.Part? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("profile_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        val reqFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", tempFile.name, reqFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewProfileUI(){
    DelcomTheme {
        ProfileUI(
            profile = ResponseUserData(
                id = "",
                name = "Ridho Alexander Pakpahan",
                username = "ifs23010",
                createdAt = "",
                updatedAt = ""
            ),
            onEditPhotoClick = {},
            onEditProfileClick = {},
            onEditPasswordClick = {}
        )
    }
}