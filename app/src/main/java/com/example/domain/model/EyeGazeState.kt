package com.example.domain.model

data class EyeGazeState(
    val lookX: Float = 0f, // -1f (left) to 1f (right)
    val lookY: Float = 0f, // -1f (up) to 1f (down)
    val blinkProgress: Float = 0f, // 0f (open) to 1f (closed)
    val audioAmplitude: Float = 0f, // 0f to 1f
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isGeneratingImage: Boolean = false,
    val isDeepSearching: Boolean = false,
    val emotion: AiEmotion = AiEmotion.CALM,
    val blendedEmotion: BlendedEmotionState = BlendedEmotionState(primary = AiEmotion.CALM),
    val speechProgress: Float = 0f,
    val isWakeWordActive: Boolean = true,
    val isAwake: Boolean = false,
    val shakeOffset: Float = 0f
)
