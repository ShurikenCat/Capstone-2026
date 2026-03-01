package com.example.capstone2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.capstone2026.ScheduleViewModel
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.capstone2026.ui.theme.Capstone2026Theme

private val navigation: Any
    get() {
        TODO()
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Capstone2026Theme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Hello World",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
                //ScheduleScreen()
                AppNavGraph()
            }
        }
    }
}

//@Composable
//fun LoginScreen(){
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var message by remember { mutableStateOf("") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ){
//        Text(
//            text = "Login",
//            style = MaterialTheme.typography.headlineMedium
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // Email Input
//        OutlinedTextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Email
//            ),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        // Password Input
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            visualTransformation = PasswordVisualTransformation(),
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Password
//            ),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // Login Button
//        Button(
//            onClick = {
//                if (email == "test@example.com" && password == "1234") {
//                    message = "Login Successful!"
//                } else {
//                    message = "Invalid credentials"
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Login")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(
//            onClick = {
//
//            },
//            modifier = Modifier.fillMaxWidth()
//        ){
//            Text("Login with Google")
//        }
//
//        // Result Message
//        if (message.isNotEmpty()) {
//            Text(
//                text = message,
//                color = MaterialTheme.colorScheme.primary
//            )
//        }
//    }
//}

@Composable
fun AppMenu(
    navController: NavController
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End
    ) {

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Home") {
                navController.navigate("home") {
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Schedule") {
                navController.navigate("schedule") {
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = isExpanded) {
            SpeedDialItem("Settings") {
                navController.navigate("settings") {
                    launchSingleTop = true
                }
                isExpanded = false
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(64.dp),   // Makes it round
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isExpanded) "×" else "+",
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        composable("home") {
            HomeScreen(navController)
        }

        composable("schedule") {
            ScheduleScreen(navController)
        }

        composable("settings") {
            SettingsScreen(navController)
        }
    }
}

@Composable
fun ScheduleScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            "Schedule Screen",
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            "Home Screen",
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            "Settings Screen",
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            AppMenu(navController)
        }
    }
}

@Composable
fun SpeedDialItem(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 12.dp
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleScreenPreview(){
    Capstone2026Theme {
        AppNavGraph()
    }
}