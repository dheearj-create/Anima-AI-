package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeepSearchScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.RobotEyesDeskScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AnimaViewModel
import com.example.viewmodel.NavigationTab
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val vm: AnimaViewModel = viewModel()
                MainAppScreen(viewModel = vm)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: AnimaViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // BackHandler: return to Robot Eyes mode
    if (uiState.currentTab != NavigationTab.ROBOT_EYES) {
        BackHandler {
            viewModel.selectTab(NavigationTab.ROBOT_EYES)
        }
    }

    val isKiosk = uiState.currentTab == NavigationTab.ROBOT_EYES && uiState.isCleanKioskMode

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        bottomBar = {
            if (!isKiosk) {
                NavigationBar(
                    containerColor = Color(0xFF0A0E17),
                    contentColor = Color(0xFF00E5FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.ROBOT_EYES,
                        onClick = { viewModel.selectTab(NavigationTab.ROBOT_EYES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.RemoveRedEye,
                                contentDescription = "Robot Eyes",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Eyes", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF00E5FF),
                            indicatorColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF718096),
                            unselectedTextColor = Color(0xFF718096)
                        ),
                        modifier = Modifier.testTag("nav_tab_eyes")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.CHAT,
                        onClick = { viewModel.selectTab(NavigationTab.CHAT) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Chat",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Chat", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF00E5FF),
                            indicatorColor = Color(0xFF00E5FF),
                            unselectedIconColor = Color(0xFF718096),
                            unselectedTextColor = Color(0xFF718096)
                        ),
                        modifier = Modifier.testTag("nav_tab_chat")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.STUDIO,
                        onClick = { viewModel.selectTab(NavigationTab.STUDIO) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Studio",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Studio", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFFFF9E00),
                            indicatorColor = Color(0xFFFF9E00),
                            unselectedIconColor = Color(0xFF718096),
                            unselectedTextColor = Color(0xFF718096)
                        ),
                        modifier = Modifier.testTag("nav_tab_studio")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.RESEARCH,
                        onClick = { viewModel.selectTab(NavigationTab.RESEARCH) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Deep Search",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Search", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFF00F5D4),
                            indicatorColor = Color(0xFF00F5D4),
                            unselectedIconColor = Color(0xFF718096),
                            unselectedTextColor = Color(0xFF718096)
                        ),
                        modifier = Modifier.testTag("nav_tab_research")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == NavigationTab.SETTINGS,
                        onClick = { viewModel.selectTab(NavigationTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("System", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color(0xFFC77DFF),
                            indicatorColor = Color(0xFFC77DFF),
                            unselectedIconColor = Color(0xFF718096),
                            unselectedTextColor = Color(0xFF718096)
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isKiosk) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    NavigationTab.ROBOT_EYES -> RobotEyesDeskScreen(viewModel = viewModel)
                    NavigationTab.CHAT -> ChatScreen(viewModel = viewModel)
                    NavigationTab.STUDIO -> ImageStudioScreen(viewModel = viewModel)
                    NavigationTab.RESEARCH -> DeepSearchScreen(viewModel = viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }

            // FULLSCREEN IMAGE PREVIEW MODAL
            uiState.previewImagePath?.let { path ->
                Dialog(
                    onDismissRequest = { viewModel.setPreviewImage(null) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.94f))
                            .clickable { viewModel.setPreviewImage(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        val file = File(path)
                        if (file.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(file)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Fullscreen Artwork",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setPreviewImage(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .background(Color(0xFF1E283C), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
