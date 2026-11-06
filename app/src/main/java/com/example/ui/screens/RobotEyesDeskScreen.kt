package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.AiEmotion
import com.example.ui.avatar.ExpressiveAvatarView
import com.example.viewmodel.AnimaViewModel
import com.example.viewmodel.NavigationTab

@Composable
fun RobotEyesDeskScreen(
    viewModel: AnimaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            viewModel.toggleCameraGazeTracking(true)
        } else {
            viewModel.toggleCameraGazeTracking(false)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "robot_ui_pulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("robot_eyes_desk_screen")
    ) {
        // 1. MAIN ONLY EYES CANVAS
        ExpressiveAvatarView(
            gazeState = uiState.gazeState,
            onAvatarTapped = { viewModel.triggerAvatarTap() },
            onGazeTrack = { x, y -> viewModel.setManualGaze(x, y) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. TOP OVERLAY HUD (Hidden in Clean Kiosk Mode)
        AnimatedVisibility(
            visible = !uiState.isCleanKioskMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wake word indicator pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF141A28).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.isWakeWordActive) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF4A5568)
                    ),
                    modifier = Modifier.clickable { viewModel.toggleWakeWordListening() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (uiState.isWakeWordActive) Color(0xFF00E5FF) else Color(0xFF718096),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isWakeWordActive) "WAKE: 'HEY ANIMA'" else "WAKE: MUTED",
                            color = if (uiState.isWakeWordActive) Color(0xFF00E5FF) else Color(0xFF718096),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Quick buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Camera Gaze Eye-Tracking Toggle
                    IconButton(
                        onClick = {
                            if (uiState.isCameraGazeEnabled) {
                                viewModel.toggleCameraGazeTracking(false)
                            } else {
                                if (hasCameraPermission) {
                                    viewModel.toggleCameraGazeTracking(true)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (uiState.isCameraGazeEnabled) Color(0xFF10B981).copy(alpha = 0.25f)
                                else Color(0xFF141A28).copy(alpha = 0.85f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (uiState.isCameraGazeEnabled) Color(0xFF10B981) else Color.Transparent,
                                CircleShape
                            )
                            .testTag("camera_gaze_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isCameraGazeEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Camera Eye Gaze Tracking",
                            tint = if (uiState.isCameraGazeEnabled) Color(0xFF10B981) else Color(0xFF718096),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.toggleVoiceEnabled() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF141A28).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voice Output",
                            tint = if (uiState.isVoiceEnabled) Color(0xFF00E5FF) else Color(0xFF718096),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.toggleCleanKioskMode() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF141A28).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Clean OLED Eyes Mode",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. FLOATING TRANSCRIPT & ACTION DOCK (Bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Speech Transcript Capsule
            AnimatedVisibility(
                visible = uiState.lastSpeechTranscript.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF121826).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28334E)),
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .testTag("speech_transcript_capsule")
                ) {
                    Text(
                        text = uiState.lastSpeechTranscript,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action bar (Hidden in clean kiosk mode)
            AnimatedVisibility(
                visible = !uiState.isCleanKioskMode,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Emotion Selector Pills (including Frustrated, Surprised, Curious)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickEmotions = listOf(
                            Triple(AiEmotion.CALM, null, "Calm"),
                            Triple(AiEmotion.SURPRISED, AiEmotion.HAPPY, "Surprised"),
                            Triple(AiEmotion.CURIOUS, AiEmotion.THINKING, "Curious"),
                            Triple(AiEmotion.FRUSTRATED, AiEmotion.MISCHIEVOUS, "Frustrated"),
                            Triple(AiEmotion.THINKING, AiEmotion.CURIOUS, "Thinking"),
                            Triple(AiEmotion.HAPPY, null, "Happy"),
                            Triple(AiEmotion.DEEP_SEARCH, null, "Deep Search")
                        )

                        quickEmotions.forEach { (pri, sec, label) ->
                            val isSelected = uiState.gazeState.emotion == pri
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) pri.primaryColor.copy(alpha = 0.25f) else Color(0xFF161D2B).copy(alpha = 0.8f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, pri.primaryColor) else null,
                                modifier = Modifier.clickable {
                                    viewModel.setEmotion(pri, sec, if (sec != null) 0.4f else 0f)
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) pri.primaryColor else Color(0xFFA0AEC0),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Center Control Dock (Wake trigger, Microphone, Chat Switcher)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Quick "Hey Anima!" Wake Tester
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF182030),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28334E)),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable {
                                    viewModel.onWakeWordTriggered()
                                    viewModel.speakText("Yes? I am listening!")
                                }
                                .testTag("wake_word_test_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Test Wake Word",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        // Large Glowing Central Microphone
                        val isListening = uiState.gazeState.isListening
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(if (isListening) micPulseScale else 1f)
                                .background(
                                    color = if (isListening) Color(0xFFFF2A6D) else Color(0xFF00E5FF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (isListening) {
                                        viewModel.stopListening()
                                    } else {
                                        if (hasMicPermission) {
                                            viewModel.startListening()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("desk_microphone_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        // Switch to full Chat/Studio tab
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF182030),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28334E)),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable { viewModel.selectTab(NavigationTab.CHAT) }
                                .testTag("open_chat_drawer_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = "Open Chat Thread",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // In Clean Kiosk mode: subtle floating restore icon
            if (uiState.isCleanKioskMode) {
                IconButton(
                    onClick = { viewModel.toggleCleanKioskMode() },
                    modifier = Modifier
                        .background(Color(0xFF141A28).copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Clean Kiosk Mode",
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
