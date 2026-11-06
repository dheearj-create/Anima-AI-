package com.example.ui.avatar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import com.example.domain.model.AiEmotion
import com.example.domain.model.EyeGazeState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cozmo / Xiaozhi OLED Animated Robot Eyes.
 * Features advanced shape morphing, eyelid cut-offs, pupil tracking,
 * emotion blending, shake on frustration, and voice reactivity.
 */
@Composable
fun ExpressiveAvatarView(
    modifier: Modifier = Modifier,
    gazeState: EyeGazeState,
    onAvatarTapped: (() -> Unit)? = null,
    onGazeTrack: ((Float, Float) -> Unit)? = null
) {
    // Smooth eye position tracking
    val animatedLookX by animateFloatAsState(
        targetValue = gazeState.lookX.coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "lookX"
    )
    val animatedLookY by animateFloatAsState(
        targetValue = gazeState.lookY.coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "lookY"
    )

    // Autonomous blinking animation
    val blinkAnim = remember { Animatable(0f) }
    LaunchedEffect(gazeState.emotion) {
        while (true) {
            val waitMs = if (gazeState.emotion.blinksFast) {
                Random.nextLong(1200, 2400)
            } else {
                Random.nextLong(2800, 5000)
            }
            delay(waitMs)
            blinkAnim.animateTo(1f, tween(85, easing = FastOutSlowInEasing))
            blinkAnim.animateTo(0f, tween(110, easing = FastOutSlowInEasing))
            if (Random.nextFloat() > 0.75f) {
                delay(90)
                blinkAnim.animateTo(1f, tween(75, easing = FastOutSlowInEasing))
                blinkAnim.animateTo(0f, tween(95, easing = FastOutSlowInEasing))
            }
        }
    }

    // Breathing pulse
    val infiniteTransition = rememberInfiniteTransition(label = "robot_life")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Frustration shake animation
    val frustrationShake by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(45, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    // Radar rotation for deep search
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    // Wake word perk-up bounce
    val wakeBounce = remember { Animatable(1f) }
    LaunchedEffect(gazeState.isAwake) {
        if (gazeState.isAwake) {
            wakeBounce.animateTo(1.22f, tween(120, easing = FastOutSlowInEasing))
            wakeBounce.animateTo(1.0f, spring(dampingRatio = 0.45f, stiffness = 600f))
        }
    }

    val blended = gazeState.blendedEmotion
    val effectiveColor = blended.effectiveColor
    val effectiveSquint = (blended.effectiveSquint + blinkAnim.value).coerceIn(0f, 1f)
    val effectivePupilScale = blended.effectivePupilScale
    val effectiveBrowTilt = blended.effectiveEyebrowTilt
    val isShaking = blended.isShaking

    val animatedSquint by animateFloatAsState(
        targetValue = effectiveSquint,
        animationSpec = spring(stiffness = 650f),
        label = "squint"
    )

    val currentShakeX = if (isShaking) frustrationShake else 0f

    var boxWidth by remember { mutableFloatStateOf(1f) }
    var boxHeight by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .onSizeChanged {
                boxWidth = it.width.toFloat().coerceAtLeast(1f)
                boxHeight = it.height.toFloat().coerceAtLeast(1f)
            }
            .testTag("expressive_robot_eyes_view")
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onAvatarTapped?.invoke()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val pos = change.position
                        // Map touch position to normalized -1f..1f gaze
                        val normX = ((pos.x / boxWidth) * 2f - 1f).coerceIn(-1f, 1f)
                        val normY = ((pos.y / boxHeight) * 2f - 1f).coerceIn(-1f, 1f)
                        onGazeTrack?.invoke(normX, normY)
                    },
                    onDragEnd = {
                        onGazeTrack?.invoke(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = this.size.width
            val canvasH = this.size.height
            val center = Offset(canvasW / 2f + currentShakeX, canvasH / 2f)

            // Pure OLED Black background
            drawRect(Color.Black)

            // Subtle Cybernetic Grid lines when deep searching
            if (gazeState.isDeepSearching) {
                drawCyberGrid(canvasW, canvasH, radarRotation)
            }

            // Audio amplitude glow halo when speaking or listening
            val audioAmp = gazeState.audioAmplitude
            if (audioAmp > 0.05f || gazeState.isSpeaking || gazeState.isListening) {
                val auraRadius = (canvasW * 0.45f) + (audioAmp * 40f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            effectiveColor.copy(alpha = 0.25f + audioAmp * 0.35f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = auraRadius
                    ),
                    radius = auraRadius,
                    center = center
                )
            }

            // Dimension definitions for Cozmo/Xiaozhi style eyes
            val eyeWidth = canvasW * 0.28f * breathScale * wakeBounce.value
            val baseEyeHeight = canvasW * 0.28f * breathScale * wakeBounce.value
            val eyeSpacing = canvasW * 0.17f

            val leftEyeCenter = Offset(center.x - eyeSpacing, center.y)
            val rightEyeCenter = Offset(center.x + eyeSpacing, center.y)

            // Draw Left Eye
            drawOledRobotEye(
                center = leftEyeCenter,
                width = eyeWidth,
                baseHeight = baseEyeHeight,
                lookX = animatedLookX,
                lookY = animatedLookY,
                squint = animatedSquint,
                browTilt = effectiveBrowTilt,
                pupilScale = effectivePupilScale,
                emotion = gazeState.emotion,
                color = effectiveColor,
                isLeft = true,
                isCurious = gazeState.emotion == AiEmotion.CURIOUS,
                audioAmp = audioAmp,
                radarAngle = radarRotation
            )

            // Draw Right Eye
            drawOledRobotEye(
                center = rightEyeCenter,
                width = eyeWidth,
                baseHeight = baseEyeHeight,
                lookX = animatedLookX,
                lookY = animatedLookY,
                squint = animatedSquint,
                browTilt = effectiveBrowTilt,
                pupilScale = effectivePupilScale,
                emotion = gazeState.emotion,
                color = effectiveColor,
                isLeft = false,
                isCurious = gazeState.emotion == AiEmotion.CURIOUS,
                audioAmp = audioAmp,
                radarAngle = radarRotation
            )

            // Wake word neon energy flare
            if (gazeState.isAwake) {
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.4f * (wakeBounce.value - 1f)),
                    radius = eyeWidth * 2.5f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

/**
 * Renders a single Xiaozhi / Cozmo style robot eye with curved squircle geometry,
 * eyelid cutoffs, specular gleam, and pupil tracking.
 */
private fun DrawScope.drawOledRobotEye(
    center: Offset,
    width: Float,
    baseHeight: Float,
    lookX: Float,
    lookY: Float,
    squint: Float,
    browTilt: Float,
    pupilScale: Float,
    emotion: AiEmotion,
    color: Color,
    isLeft: Boolean,
    isCurious: Boolean,
    audioAmp: Float,
    radarAngle: Float
) {
    // If emotion is curious, the left or right eye arches slightly higher & larger!
    val curiousScale = if (isCurious && isLeft) 1.15f else if (isCurious && !isLeft) 0.92f else 1.0f
    val curiousYOffset = if (isCurious && isLeft) -baseHeight * 0.08f else 0f

    val effectiveW = width * curiousScale
    val effectiveH = (baseHeight * (1f - squint * 0.85f) * curiousScale).coerceAtLeast(8f)

    val eyeTop = center.y - (effectiveH / 2f) + curiousYOffset
    val eyeBottom = center.y + (effectiveH / 2f) + curiousYOffset
    val eyeLeft = center.x - (effectiveW / 2f)
    val eyeRight = center.x + (effectiveW / 2f)

    // Complete blink slit
    if (effectiveH <= 12f) {
        drawLine(
            color = color,
            start = Offset(eyeLeft, center.y + curiousYOffset),
            end = Offset(eyeRight, center.y + curiousYOffset),
            strokeWidth = 6f
        )
        return
    }

    // 1. Base Squircle Eye Capsule
    val cornerRadius = when {
        emotion == AiEmotion.SURPRISED -> effectiveW * 0.48f // Ultra-wide round circular eyes
        emotion == AiEmotion.HAPPY -> effectiveW * 0.38f
        else -> effectiveW * 0.32f
    }

    // 2. Build Eye Path with Eyelid Cut-offs
    val eyePath = Path().apply {
        addRoundRect(
            RoundRect(
                left = eyeLeft,
                top = eyeTop,
                right = eyeRight,
                bottom = eyeBottom,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }

    // If HAPPY: bottom eyelid cuts up into inverted smiling crescent ^_^
    val finalEyePath = if (emotion == AiEmotion.HAPPY) {
        val smileCutPath = Path().apply {
            val smileH = effectiveH * 0.52f
            moveTo(eyeLeft - 10f, eyeBottom + 10f)
            quadraticTo(
                center.x, eyeBottom - smileH,
                eyeRight + 10f, eyeBottom + 10f
            )
            lineTo(eyeRight + 10f, eyeBottom + 30f)
            lineTo(eyeLeft - 10f, eyeBottom + 30f)
            close()
        }
        val diff = Path()
        diff.op(eyePath, smileCutPath, PathOperation.Difference)
        diff
    } else if (browTilt != 0f) {
        // Angled brow cut: tilts down inside (frustrated/focused) or up inside (surprised/curious)
        val cutPath = Path().apply {
            val tiltAmount = effectiveH * 0.45f * browTilt
            val innerTilt = if (isLeft) tiltAmount else -tiltAmount
            moveTo(eyeLeft - 10f, eyeTop - 40f)
            lineTo(eyeRight + 10f, eyeTop - 40f)
            lineTo(eyeRight + 10f, eyeTop + (if (isLeft) -innerTilt else innerTilt))
            lineTo(eyeLeft - 10f, eyeTop + (if (isLeft) innerTilt else -innerTilt))
            close()
        }
        val diff = Path()
        diff.op(eyePath, cutPath, PathOperation.Difference)
        diff
    } else {
        eyePath
    }

    // 3. Draw Outer Neon Glow
    drawPath(
        path = finalEyePath,
        color = color.copy(alpha = 0.35f + audioAmp * 0.4f),
        style = Stroke(width = 10f)
    )

    // 4. Draw Main Solid Neon Eye Body
    drawPath(
        path = finalEyePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.85f)
            ),
            startY = eyeTop,
            endY = eyeBottom
        )
    )

    // 5. Inner Pupil & Gaze Shift
    val maxShiftX = effectiveW * 0.22f
    val maxShiftY = effectiveH * 0.22f
    val pupilX = center.x + (lookX * maxShiftX)
    val pupilY = center.y + curiousYOffset + (lookY * maxShiftY)

    // Specular Catchlight Sparkle (gives that lively Pixar / Cozmo expression)
    val gleamRadius = (effectiveW * 0.16f * pupilScale).coerceIn(4f, effectiveW * 0.3f)
    val gleamOffset = Offset(
        pupilX + if (isLeft) -gleamRadius * 0.3f else gleamRadius * 0.3f,
        pupilY - gleamRadius * 0.35f
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = gleamRadius,
        center = gleamOffset
    )

    // Secondary smaller shine
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = gleamRadius * 0.45f,
        center = Offset(gleamOffset.x + (if (isLeft) gleamRadius * 0.9f else -gleamRadius * 0.9f), gleamOffset.y + gleamRadius * 0.85f)
    )

    // Deep Search Scanning Line inside the eye
    if (emotion == AiEmotion.DEEP_SEARCH) {
        val scanY = eyeTop + ((radarAngle % 100f) / 100f) * effectiveH
        drawLine(
            color = Color(0xFF00F5D4),
            start = Offset(eyeLeft, scanY),
            end = Offset(eyeRight, scanY),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawCyberGrid(w: Float, h: Float, rotation: Float) {
    val gridColor = Color(0xFF00F5D4).copy(alpha = 0.08f)
    val step = 45f
    var x = 0f
    while (x < w) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < h) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
        y += step
    }
}
