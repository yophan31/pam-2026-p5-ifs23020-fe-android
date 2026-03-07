package org.delcom.pam_p5_ifs23020

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.delcom.pam_p5_ifs23020.ui.UIApp
import org.delcom.pam_p5_ifs23020.ui.theme.DelcomTheme
import org.delcom.pam_p5_ifs23020.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23020.ui.viewmodels.TodoViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val todoViewModel: TodoViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DelcomTheme {
                UIApp(
                    todoViewModel = todoViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}