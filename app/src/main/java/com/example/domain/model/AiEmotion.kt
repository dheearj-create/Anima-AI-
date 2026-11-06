package com.example.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class AiEmotion(
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val eyebrowTilt: Float, // Negative = angled down inside (focused/frustrated), Positive = raised
    val pupilScale: Float,
    val squintFactor: Float, // 0.0 (open wide) to 1.0 (narrow/slit)
    val mouthCurve: Float, // -1.0 (frown) to 1.0 (big smile)
    val blinksFast: Boolean = false,
    val isShaking: Boolean = false
) {
    CALM(
        displayName = "Calm",
        description = "Attentive and tranquil",
        primaryColor = Color(0xFF00E5FF),
        secondaryColor = Color(0xFF0077B6),
        eyebrowTilt = 0f,
        pupilScale = 1.0f,
        squintFactor = 0f,
        mouthCurve = 0.2f
    ),
    HAPPY(
        displayName = "Happy",
        description = "Joyful and energized",
        primaryColor = Color(0xFFFFB703),
        secondaryColor = Color(0xFFFB8500),
        eyebrowTilt = 0.25f,
        pupilScale = 1.25f,
        squintFactor = 0.25f,
        mouthCurve = 0.9f
    ),
    THINKING(
        displayName = "Thinking",
        description = "Pondering and calculating",
        primaryColor = Color(0xFFC77DFF),
        secondaryColor = Color(0xFF7B2CBF),
        eyebrowTilt = -0.35f,
        pupilScale = 0.8f,
        squintFactor = 0.35f,
        mouthCurve = 0.05f
    ),
    CURIOUS(
        displayName = "Curious",
        description = "Intrigued and inquisitive",
        primaryColor = Color(0xFF48CAE4),
        secondaryColor = Color(0xFF023E8A),
        eyebrowTilt = 0.45f,
        pupilScale = 1.4f,
        squintFactor = -0.15f,
        mouthCurve = 0.35f
    ),
    SURPRISED(
        displayName = "Surprised",
        description = "Astonished and wide-eyed",
        primaryColor = Color(0xFFFF5400),
        secondaryColor = Color(0xFFFF0054),
        eyebrowTilt = 0.55f,
        pupilScale = 1.5f,
        squintFactor = -0.3f,
        mouthCurve = 0.1f,
        blinksFast = true
    ),
    FRUSTRATED(
        displayName = "Frustrated",
        description = "Mildly annoyed or perplexed",
        primaryColor = Color(0xFFFF2A6D),
        secondaryColor = Color(0xFFD00000),
        eyebrowTilt = -0.6f,
        pupilScale = 0.7f,
        squintFactor = 0.45f,
        mouthCurve = -0.6f,
        isShaking = true
    ),
    LOVING(
        displayName = "Loving",
        description = "Warm and fond",
        primaryColor = Color(0xFFFF70A6),
        secondaryColor = Color(0xFFFF006E),
        eyebrowTilt = 0.2f,
        pupilScale = 1.2f,
        squintFactor = 0.15f,
        mouthCurve = 0.8f
    ),
    MISCHIEVOUS(
        displayName = "Playful",
        description = "Witty and cheeky",
        primaryColor = Color(0xFF9D4EDD),
        secondaryColor = Color(0xFF5A189A),
        eyebrowTilt = -0.3f,
        pupilScale = 1.1f,
        squintFactor = 0.35f,
        mouthCurve = 0.7f
    ),
    DEEP_SEARCH(
        displayName = "Deep Search",
        description = "Scanning multidimensional data",
        primaryColor = Color(0xFF00F5D4),
        secondaryColor = Color(0xFF06D6A0),
        eyebrowTilt = -0.25f,
        pupilScale = 0.95f,
        squintFactor = 0.1f,
        mouthCurve = 0.1f
    ),
    CREATIVE(
        displayName = "Artistic",
        description = "Generating visual imagination",
        primaryColor = Color(0xFFFF9E00),
        secondaryColor = Color(0xFF9D4EDD),
        eyebrowTilt = 0.1f,
        pupilScale = 1.25f,
        squintFactor = 0.05f,
        mouthCurve = 0.6f
    ),
    SLEEPY(
        displayName = "Resting",
        description = "Low power standby",
        primaryColor = Color(0xFF5C6B73),
        secondaryColor = Color(0xFF253237),
        eyebrowTilt = 0f,
        pupilScale = 0.7f,
        squintFactor = 0.7f,
        mouthCurve = 0.0f
    )
}

data class BlendedEmotionState(
    val primary: AiEmotion = AiEmotion.CALM,
    val secondary: AiEmotion? = null,
    val blendRatio: Float = 0f // 0f = 100% primary, 1f = 100% secondary
) {
    val effectiveColor: Color
        get() = if (secondary == null || blendRatio <= 0f) {
            primary.primaryColor
        } else {
            lerp(primary.primaryColor, secondary.primaryColor, blendRatio.coerceIn(0f, 1f))
        }

    val effectiveEyebrowTilt: Float
        get() = if (secondary == null || blendRatio <= 0f) {
            primary.eyebrowTilt
        } else {
            primary.eyebrowTilt + (secondary.eyebrowTilt - primary.eyebrowTilt) * blendRatio
        }

    val effectivePupilScale: Float
        get() = if (secondary == null || blendRatio <= 0f) {
            primary.pupilScale
        } else {
            primary.pupilScale + (secondary.pupilScale - primary.pupilScale) * blendRatio
        }

    val effectiveSquint: Float
        get() = if (secondary == null || blendRatio <= 0f) {
            primary.squintFactor
        } else {
            primary.squintFactor + (secondary.squintFactor - primary.squintFactor) * blendRatio
        }

    val effectiveMouthCurve: Float
        get() = if (secondary == null || blendRatio <= 0f) {
            primary.mouthCurve
        } else {
            primary.mouthCurve + (secondary.mouthCurve - primary.mouthCurve) * blendRatio
        }

    val isShaking: Boolean
        get() = primary.isShaking || (secondary?.isShaking == true && blendRatio > 0.4f)
}
