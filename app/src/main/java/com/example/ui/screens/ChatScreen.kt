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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.DeepSearchProgressCard
import com.example.ui.components.MessageBubble
import com.example.viewmodel.AnimaViewModel

@Composable
fun ChatScreen(
    viewModel: AnimaViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
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

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.deepSearchSteps.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Audio visualizer pulse for mic
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .imePadding()
    ) {
        // TOP BAR: App Identity, Offline Badge, Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "100% OFFLINE SECURE",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.toggleVoiceEnabled() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle voice output",
                        tint = if (uiState.isVoiceEnabled) Color(0xFF00E5FF) else Color(0xFF718096),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear conversation",
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // COMPACT EXPRESSIVE AVATAR HEADER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpressiveAvatarView(
                gazeState = uiState.gazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                onAvatarTapped = { viewModel.triggerAvatarTap() },
                onGazeTrack = { x, y -> viewModel.setManualGaze(x, y) }
            )

            // Emotion quick-switch selector pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiEmotion.values().forEach { emotion ->
                    val isSelected = uiState.gazeState.emotion == emotion
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) emotion.primaryColor.copy(alpha = 0.25f) else Color(0xFF141926),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, emotion.primaryColor) else null,
                        modifier = Modifier
                            .clickable { viewModel.setEmotion(emotion) }
                            .testTag("emotion_pill_${emotion.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(emotion.primaryColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = emotion.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) emotion.primaryColor else Color(0xFFA0AEC0),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }

        // SPEECH LISTENING BANNER (When mic is active)
        AnimatedVisibility(
            visible = uiState.gazeState.isListening,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF003844)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Listening",
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier
                            .size(22.dp)
                            .scale(micPulseScale)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Listening to your voice...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF00F5D4),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Speak now. Anima's eyes follow your voice amplitude.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFB2F5EA)
                            )
                        )
                    }
                }
            }
        }

        // DEEP SEARCH LIVE SCANNER CARD
        if (uiState.isDeepSearching) {
            DeepSearchProgressCard(
                steps = uiState.deepSearchSteps,
                query = uiState.activeDeepSearchQuery
            )
        }

        // CHAT MESSAGE STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onSpeak = { text -> viewModel.speakText(text) },
                    onImageClick = { path -> viewModel.setPreviewImage(path) }
                )
            }
        }

        // BOTTOM INPUT DOCK
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131824))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Controls Row: Deep Search Toggle Pill + Image Shortcut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Deep search toggle button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isDeepSearchToggle) Color(0xFF00F5D4).copy(alpha = 0.2f) else Color(0xFF1C2436),
                        border = if (uiState.isDeepSearchToggle) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4)) else null,
                        modifier = Modifier
                            .clickable { viewModel.toggleDeepSearchMode() }
                            .testTag("deep_search_toggle_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Deep Search Mode",
                                tint = if (uiState.isDeepSearchToggle) Color(0xFF00F5D4) else Color(0xFF718096),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isDeepSearchToggle) "Deep Search ON" else "Deep Search",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (uiState.isDeepSearchToggle) Color(0xFF00F5D4) else Color(0xFF718096),
                                    fontWeight = if (uiState.isDeepSearchToggle) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    // Image hint pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1C2436),
                        modifier = Modifier.clickable {
                            viewModel.onInputTextChanged("generate image of a futuristic neon cyber temple in the mountains")
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Image Hint",
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Try: 'paint / draw...'",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFFB703),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Input row with Text Field, Mic Button, and Send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        placeholder = {
                            Text(
                                text = if (uiState.isDeepSearchToggle) "Ask for deep research & analysis..." else "Chat, ask to draw, or speak...",
                                color = Color(0xFF718096),
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.isDeepSearchToggle) Color(0xFF00F5D4) else Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF1A2234),
                            unfocusedContainerColor = Color(0xFF1A2234),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Microphone Button
                    val isListening = uiState.gazeState.isListening
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (isListening) micPulseScale else 1f)
                            .background(
                                color = if (isListening) Color(0xFFFF0054) else Color(0xFF1F293D),
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
                            .testTag("microphone_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Voice microphone input",
                            tint = if (isListening) Color.White else Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (uiState.isDeepSearchToggle) Color(0xFF00F5D4) else Color(0xFF00B4D8),
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.sendMessage()
                            }
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
