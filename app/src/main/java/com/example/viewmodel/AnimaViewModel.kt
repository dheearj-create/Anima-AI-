package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.DeepSearchDossierEntity
import com.example.data.local.entity.GeneratedImageEntity
import com.example.data.repository.ChatRepository
import com.example.domain.engine.AiResponse
import com.example.domain.engine.DeepSearchAgent
import com.example.domain.engine.DeepSearchStep
import com.example.domain.engine.GenerativeArtEngine
import com.example.domain.engine.OfflineKnowledgeEngine
import com.example.domain.model.AiEmotion
import com.example.domain.model.BlendedEmotionState
import com.example.domain.model.EyeGazeState
import com.example.speech.SpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class NavigationTab {
    ROBOT_EYES, // Only Eyes Desk Robot mode (Xiaozhi/Cozmo style)
    CHAT,
    STUDIO,
    RESEARCH,
    SETTINGS
}

data class AnimaUiState(
    val currentTab: NavigationTab = NavigationTab.ROBOT_EYES,
    val gazeState: EyeGazeState = EyeGazeState(),
    val messages: List<ChatMessageEntity> = emptyList(),
    val images: List<GeneratedImageEntity> = emptyList(),
    val dossiers: List<DeepSearchDossierEntity> = emptyList(),
    val inputText: String = "",
    val isDeepSearchToggle: Boolean = false,
    val isDeepSearching: Boolean = false,
    val deepSearchSteps: List<DeepSearchStep> = emptyList(),
    val activeDeepSearchQuery: String = "",
    val isGeneratingImage: Boolean = false,
    val previewImagePath: String? = null,
    val viewedDossier: DeepSearchDossierEntity? = null,
    val isVoiceEnabled: Boolean = true,
    val speechRecognitionAvailable: Boolean = true,
    val isOfflineModeActive: Boolean = true,
    val lastSpeechTranscript: String = "Say 'Hey Anima' or tap the mic",
    val isCleanKioskMode: Boolean = false,
    val isWakeWordActive: Boolean = true,
    val isCameraGazeEnabled: Boolean = false,
    val cameraGazeData: com.example.camera.CameraGazeData = com.example.camera.CameraGazeData(),
    val elevenLabsApiKey: String = "",
    val isElevenLabsActive: Boolean = false,
    val elevenLabsVoiceId: String = "MzqUf1HbJ8UmQ0wUsx2p"
)

class AnimaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = ChatRepository(application, database)
    val speechManager = SpeechManager(application)
    val cameraGazeTracker = com.example.camera.CameraGazeTracker(application)

    private val _uiState = MutableStateFlow(AnimaUiState())
    val uiState: StateFlow<AnimaUiState> = _uiState.asStateFlow()

    private var autonomousGazeJob: Job? = null
    private var deepSearchJob: Job? = null
    private var wakeResetJob: Job? = null

    init {
        // Observe CameraGazeTracker
        viewModelScope.launch {
            cameraGazeTracker.gazeFlow.collect { gazeData ->
                _uiState.value = _uiState.value.copy(cameraGazeData = gazeData)
                // When camera tracking is active and face is detected, dynamically drive robot eye gaze
                if (_uiState.value.isCameraGazeEnabled && gazeData.isFaceDetected) {
                    val curr = _uiState.value.gazeState
                    _uiState.value = _uiState.value.copy(
                        gazeState = curr.copy(
                            lookX = gazeData.gazeX,
                            lookY = gazeData.gazeY
                        )
                    )
                }
            }
        }
        // Observe messages, images, and dossiers from Room
        viewModelScope.launch {
            repository.allMessages.collect { msgList ->
                _uiState.value = _uiState.value.copy(messages = msgList)
                if (msgList.isEmpty()) {
                    repository.insertAiMessage(
                        text = "Hello! I am Anima. My eyes track your voice and gestures in real time! Say 'Hey Anima' to wake me, ask me to create digital artwork, or switch on Deep Search for rigorous offline investigation.",
                        emotion = AiEmotion.HAPPY.name
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.allImages.collect { imgList ->
                _uiState.value = _uiState.value.copy(images = imgList)
            }
        }

        viewModelScope.launch {
            repository.allDossiers.collect { dossierList ->
                _uiState.value = _uiState.value.copy(dossiers = dossierList)
            }
        }

        // Connect SpeechManager callbacks & state
        speechManager.onSpeechRecognized = { recognizedText ->
            _uiState.value = _uiState.value.copy(lastSpeechTranscript = "You: \"$recognizedText\"")
            sendMessage(recognizedText)
        }

        speechManager.onWakeWordDetected = {
            onWakeWordTriggered()
        }

        viewModelScope.launch {
            combine(
                speechManager.isListening,
                speechManager.isSpeaking,
                speechManager.audioAmplitude
            ) { isListening, isSpeaking, amp ->
                Triple(isListening, isSpeaking, amp)
            }.collect { (isListening, isSpeaking, amp) ->
                val currentGaze = _uiState.value.gazeState
                val updatedGaze = currentGaze.copy(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    audioAmplitude = amp,
                    lookX = if (isListening) 0f else currentGaze.lookX,
                    lookY = if (isListening) -0.1f else currentGaze.lookY
                )
                _uiState.value = _uiState.value.copy(gazeState = updatedGaze)
            }
        }

        startAutonomousSaccades()
    }

    fun onWakeWordTriggered() {
        wakeResetJob?.cancel()
        _uiState.value = _uiState.value.copy(
            lastSpeechTranscript = "⚡ Anima: 'I'm listening!'",
            gazeState = _uiState.value.gazeState.copy(
                isAwake = true,
                emotion = AiEmotion.SURPRISED,
                blendedEmotion = BlendedEmotionState(
                    primary = AiEmotion.SURPRISED,
                    secondary = AiEmotion.HAPPY,
                    blendRatio = 0.5f
                ),
                lookX = 0f,
                lookY = -0.15f
            )
        )
        wakeResetJob = viewModelScope.launch {
            delay(3200)
            val curr = _uiState.value.gazeState
            _uiState.value = _uiState.value.copy(
                gazeState = curr.copy(
                    isAwake = false,
                    emotion = AiEmotion.CALM,
                    blendedEmotion = BlendedEmotionState(primary = AiEmotion.CALM)
                )
            )
        }
    }

    private fun startAutonomousSaccades() {
        autonomousGazeJob?.cancel()
        autonomousGazeJob = viewModelScope.launch {
            while (true) {
                delay(Random.nextLong(2000, 4200))
                val currentGaze = _uiState.value.gazeState
                if (!currentGaze.isListening && !currentGaze.isDeepSearching && !currentGaze.isAwake) {
                    val saccadeX = (Random.nextFloat() * 0.5f - 0.25f)
                    val saccadeY = (Random.nextFloat() * 0.35f - 0.17f)
                    _uiState.value = _uiState.value.copy(
                        gazeState = currentGaze.copy(
                            lookX = saccadeX,
                            lookY = saccadeY
                        )
                    )
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText)
        if (newText.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                gazeState = _uiState.value.gazeState.copy(
                    lookX = (newText.length % 5 - 2) * 0.08f,
                    lookY = 0.35f,
                    emotion = AiEmotion.CURIOUS,
                    blendedEmotion = BlendedEmotionState(
                        primary = AiEmotion.CURIOUS,
                        secondary = AiEmotion.THINKING,
                        blendRatio = 0.3f
                    )
                )
            )
        }
    }

    fun toggleDeepSearchMode() {
        val newToggle = !_uiState.value.isDeepSearchToggle
        _uiState.value = _uiState.value.copy(
            isDeepSearchToggle = newToggle,
            gazeState = _uiState.value.gazeState.copy(
                emotion = if (newToggle) AiEmotion.DEEP_SEARCH else AiEmotion.CALM,
                blendedEmotion = BlendedEmotionState(
                    primary = if (newToggle) AiEmotion.DEEP_SEARCH else AiEmotion.CALM
                )
            )
        )
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        when (tab) {
            NavigationTab.ROBOT_EYES -> {
                setEmotion(AiEmotion.CALM)
                if (_uiState.value.isWakeWordActive) {
                    speechManager.setWakeWordListening(true)
                }
            }
            NavigationTab.CHAT -> setEmotion(AiEmotion.CALM)
            NavigationTab.STUDIO -> setEmotion(AiEmotion.CREATIVE)
            NavigationTab.RESEARCH -> setEmotion(AiEmotion.DEEP_SEARCH)
            NavigationTab.SETTINGS -> setEmotion(AiEmotion.THINKING)
        }
    }

    fun toggleCleanKioskMode() {
        _uiState.value = _uiState.value.copy(isCleanKioskMode = !_uiState.value.isCleanKioskMode)
    }

    fun toggleWakeWordListening() {
        val newActive = !_uiState.value.isWakeWordActive
        _uiState.value = _uiState.value.copy(isWakeWordActive = newActive)
        speechManager.setWakeWordListening(newActive)
    }

    fun toggleVoiceEnabled() {
        val newVoice = !_uiState.value.isVoiceEnabled
        speechManager.isVoiceEnabled = newVoice
        if (!newVoice) {
            speechManager.stopSpeaking()
        }
        _uiState.value = _uiState.value.copy(isVoiceEnabled = newVoice)
    }

    fun setEmotion(emotion: AiEmotion, secondary: AiEmotion? = null, blendRatio: Float = 0f) {
        _uiState.value = _uiState.value.copy(
            gazeState = _uiState.value.gazeState.copy(
                emotion = emotion,
                blendedEmotion = BlendedEmotionState(
                    primary = emotion,
                    secondary = secondary,
                    blendRatio = blendRatio
                )
            )
        )
    }

    fun setManualGaze(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(
            gazeState = _uiState.value.gazeState.copy(lookX = x, lookY = y)
        )
    }

    fun triggerAvatarTap() {
        wakeResetJob?.cancel()
        speechManager.playWakeChime()

        val tapEmotions = listOf(
            Pair(AiEmotion.SURPRISED, AiEmotion.HAPPY),
            Pair(AiEmotion.HAPPY, AiEmotion.MISCHIEVOUS),
            Pair(AiEmotion.CURIOUS, AiEmotion.SURPRISED),
            Pair(AiEmotion.FRUSTRATED, AiEmotion.MISCHIEVOUS)
        )
        val (pri, sec) = tapEmotions.random()
        setEmotion(pri, sec, 0.45f)

        val soundLines = listOf(
            "Hehe, boop! My optical sensors are ticklish!",
            "I'm awake and watching your touch!",
            "Yes! My neural circuits are purring.",
            "Hey! What are we doing next?"
        )
        val line = soundLines.random()
        _uiState.value = _uiState.value.copy(lastSpeechTranscript = line)
        speechManager.speak(line)

        wakeResetJob = viewModelScope.launch {
            delay(2800)
            setEmotion(AiEmotion.CALM)
        }
    }

    fun startListening() {
        speechManager.playWakeChime()
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun speakText(text: String) {
        speechManager.speak(text)
    }

    fun sendMessage(text: String = _uiState.value.inputText) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _uiState.value = _uiState.value.copy(inputText = "")

        viewModelScope.launch {
            repository.insertUserMessage(trimmed)

            // Look up-right in thought (narrowing eyes)
            _uiState.value = _uiState.value.copy(
                gazeState = _uiState.value.gazeState.copy(
                    emotion = AiEmotion.THINKING,
                    blendedEmotion = BlendedEmotionState(
                        primary = AiEmotion.THINKING,
                        secondary = AiEmotion.CURIOUS,
                        blendRatio = 0.25f
                    ),
                    lookX = -0.25f,
                    lookY = -0.35f
                )
            )

            // Process via offline engine
            val response = OfflineKnowledgeEngine.processMessage(
                rawInput = trimmed,
                isDeepSearchToggleActive = _uiState.value.isDeepSearchToggle
            )

            when {
                response.isImagePrompt -> {
                    handleImageGeneration(response.imagePromptText ?: trimmed, response)
                }
                response.isDeepSearchRequested -> {
                    handleDeepSearch(response.deepSearchQuery ?: trimmed, response)
                }
                else -> {
                    delay(380)
                    _uiState.value = _uiState.value.copy(
                        lastSpeechTranscript = "Anima: \"${response.replyText.take(120)}...\"",
                        gazeState = _uiState.value.gazeState.copy(
                            emotion = response.emotion,
                            blendedEmotion = BlendedEmotionState(
                                primary = response.emotion,
                                secondary = response.secondaryEmotion,
                                blendRatio = response.blendRatio
                            ),
                            lookX = response.suggestedGazeX,
                            lookY = response.suggestedGazeY
                        )
                    )
                    repository.insertAiMessage(
                        text = response.replyText,
                        emotion = response.emotion.name
                    )
                    speechManager.speak(response.replyText)
                }
            }
        }
    }

    private suspend fun handleImageGeneration(prompt: String, response: AiResponse) {
        _uiState.value = _uiState.value.copy(
            isGeneratingImage = true,
            lastSpeechTranscript = "🎨 Anima: 'Synthesizing artwork: $prompt'",
            gazeState = _uiState.value.gazeState.copy(
                emotion = AiEmotion.CREATIVE,
                blendedEmotion = BlendedEmotionState(primary = AiEmotion.CREATIVE, secondary = AiEmotion.HAPPY, blendRatio = 0.3f),
                isGeneratingImage = true
            )
        )

        val imageEntity = repository.generateArtwork(
            prompt = prompt,
            style = GenerativeArtEngine.ArtStyle.CYBERPUNK
        )

        _uiState.value = _uiState.value.copy(
            isGeneratingImage = false,
            lastSpeechTranscript = "Artwork ready in Studio!",
            gazeState = _uiState.value.gazeState.copy(
                emotion = AiEmotion.HAPPY,
                blendedEmotion = BlendedEmotionState(primary = AiEmotion.HAPPY),
                isGeneratingImage = false
            )
        )

        repository.insertAiMessage(
            text = response.replyText,
            emotion = AiEmotion.CREATIVE.name,
            imageUrl = imageEntity.imagePath
        )

        speechManager.speak("I've painted your visual creation. It's ready in your gallery!")
    }

    private fun handleDeepSearch(query: String, response: AiResponse) {
        deepSearchJob?.cancel()
        deepSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeepSearching = true,
                activeDeepSearchQuery = query,
                lastSpeechTranscript = "🔬 Deep Searching: $query",
                gazeState = _uiState.value.gazeState.copy(
                    emotion = AiEmotion.DEEP_SEARCH,
                    blendedEmotion = BlendedEmotionState(primary = AiEmotion.DEEP_SEARCH, secondary = AiEmotion.THINKING, blendRatio = 0.35f),
                    isDeepSearching = true,
                    lookX = -0.3f
                )
            )

            DeepSearchAgent.executeDeepSearch(query).collect { (steps, finalReport) ->
                _uiState.value = _uiState.value.copy(deepSearchSteps = steps)

                if (finalReport != null) {
                    val dossierId = repository.saveDeepSearchDossier(finalReport)

                    _uiState.value = _uiState.value.copy(
                        isDeepSearching = false,
                        deepSearchSteps = emptyList(),
                        lastSpeechTranscript = "Dossier compiled for $query",
                        gazeState = _uiState.value.gazeState.copy(
                            emotion = AiEmotion.CALM,
                            blendedEmotion = BlendedEmotionState(primary = AiEmotion.CALM),
                            isDeepSearching = false
                        )
                    )

                    repository.insertAiMessage(
                        text = finalReport.reportMarkdown,
                        emotion = AiEmotion.DEEP_SEARCH.name,
                        isDeepSearch = true,
                        deepSearchDossierId = dossierId
                    )

                    speechManager.speak("Deep Search intelligence dossier finalized for $query.")
                }
            }
        }
    }

    fun generateStudioImage(prompt: String, style: GenerativeArtEngine.ArtStyle) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingImage = true,
                gazeState = _uiState.value.gazeState.copy(
                    emotion = AiEmotion.CREATIVE,
                    blendedEmotion = BlendedEmotionState(primary = AiEmotion.CREATIVE),
                    isGeneratingImage = true
                )
            )

            val imageEntity = repository.generateArtwork(prompt, style)

            _uiState.value = _uiState.value.copy(
                isGeneratingImage = false,
                previewImagePath = imageEntity.imagePath,
                gazeState = _uiState.value.gazeState.copy(
                    emotion = AiEmotion.HAPPY,
                    blendedEmotion = BlendedEmotionState(primary = AiEmotion.HAPPY),
                    isGeneratingImage = false
                )
            )
            speechManager.speak("Your artwork in style ${style.label} has been generated!")
        }
    }

    fun setPreviewImage(path: String?) {
        _uiState.value = _uiState.value.copy(previewImagePath = path)
    }

    fun setViewedDossier(dossier: DeepSearchDossierEntity?) {
        _uiState.value = _uiState.value.copy(viewedDossier = dossier)
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteImage(id: Long) {
        viewModelScope.launch {
            repository.deleteImage(id)
        }
    }

    fun deleteDossier(id: Long) {
        viewModelScope.launch {
            repository.deleteDossier(id)
        }
    }

    fun setElevenLabsApiKey(key: String) {
        speechManager.customApiKey = key
        _uiState.value = _uiState.value.copy(
            elevenLabsApiKey = key,
            isElevenLabsActive = speechManager.isElevenLabsActive
        )
    }

    fun testElevenLabsVoice() {
        speechManager.speak("Hello! I am Anima, speaking with ElevenLabs voice ID MzqUf1HbJ8UmQ0wUsx2p. My vocal synthesis is online!")
    }

    fun toggleCameraGazeTracking(enabled: Boolean? = null) {
        val target = enabled ?: !_uiState.value.isCameraGazeEnabled
        _uiState.value = _uiState.value.copy(isCameraGazeEnabled = target)
        if (target) {
            cameraGazeTracker.startTracking()
        } else {
            cameraGazeTracker.stopTracking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
        cameraGazeTracker.stopTracking()
        autonomousGazeJob?.cancel()
        deepSearchJob?.cancel()
        wakeResetJob?.cancel()
    }
}
