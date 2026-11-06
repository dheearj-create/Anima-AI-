package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.camera.CameraGazeData
import com.example.domain.model.AiEmotion
import com.example.domain.model.EyeGazeState
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Reusable GazeEye component.
 * Uses front-camera sensor data (CameraGazeData) to track user eye movements
 * and simulate looking back at the user on the screen.
 */
@Composable
fun GazeEye(
    modifier: Modifier = Modifier,
    isLeft: Boolean = true,
    gazeState: EyeGazeState = EyeGazeState(),
    cameraGazeData: CameraGazeData? = null,
    useCameraTracking: Boolean = true,
    color: Color = Color(0xFF00E5FF),
    emotion: AiEmotion = AiEmotion.CALM,
    onEyeTapped: (() -> Unit)? = null
) {
    // Target eye gaze coordinates: prioritize camera tracking if available
    val targetGazeX = if (useCameraTracking && cameraGazeData?.isFaceDetected == true) {
        cameraGazeData.gazeX
    } else {
        gazeState.lookX
    }

    val targetGazeY = if (useCameraTracking && cameraGazeData?.isFaceDetected == true) {
        cameraGazeData.gazeY
    } else {
        gazeState.lookY
    }

    // Smooth physics-based tracking
    val animatedGazeX by animateFloatAsState(
        targetValue = targetGazeX.coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 550f),
        label = "gazeX"
    )
    val animatedGazeY by animateFloatAsState(
        targetValue = targetGazeY.coerceIn(-1f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 550f),
        label = "gazeY"
    )

    // Autonomous blink loop
    val blinkAnim = remember { Animatable(0f) }
    LaunchedEffect(emotion) {
        while (true) {
            val wait = if (emotion.blinksFast) Random.nextLong(1200, 2400) else Random.nextLong(2800, 5200)
            delay(wait)
            blinkAnim.animateTo(1f, tween(85, easing = FastOutSlowInEasing))
            blinkAnim.animateTo(0f, tween(110, easing = FastOutSlowInEasing))
            if (Random.nextFloat() > 0.75f) {
                delay(80)
                blinkAnim.animateTo(1f, tween(75, easing = FastOutSlowInEasing))
                blinkAnim.animateTo(0f, tween(95, easing = FastOutSlowInEasing))
            }
        }
    }

    // Breathing pulse
    val infiniteTransition = rememberInfiniteTransition(label = "eye_life")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Eye squint factoring
    val blendedSquint = (gazeState.blendedEmotion.effectiveSquint + blinkAnim.value).coerceIn(0f, 1f)
    val animatedSquint by animateFloatAsState(
        targetValue = blendedSquint,
        animationSpec = spring(stiffness = 650f),
        label = "squint"
    )

    val browTilt = gazeState.blendedEmotion.effectiveEyebrowTilt
    val pupilScale = gazeState.blendedEmotion.effectivePupilScale
    val effectiveColor = gazeState.blendedEmotion.effectiveColor

    Box(
        modifier = modifier
            .testTag(if (isLeft) "gaze_eye_left" else "gaze_eye_right")
            .pointerInput(Unit) {
                detectTapGestures {
                    onEyeTapped?.invoke()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Scaled dimensions
            val eyeW = w * 0.88f * breathScale
            val baseH = h * 0.88f * breathScale
            val effectiveH = (baseH * (1f - animatedSquint * 0.85f)).coerceAtLeast(6f)

            val eyeTop = center.y - (effectiveH / 2f)
            val eyeBottom = center.y + (effectiveH / 2f)
            val eyeLeft = center.x - (eyeW / 2f)
            val eyeRight = center.x + (eyeW / 2f)

            if (effectiveH <= 10f) {
                // Eyelid slit when blinking
                drawLine(
                    color = effectiveColor,
                    start = Offset(eyeLeft, center.y),
                    end = Offset(eyeRight, center.y),
                    strokeWidth = 6f
                )
                return@Canvas
            }

            // 1. Squircle Corner Radius based on emotion
            val cornerRadius = when {
                emotion == AiEmotion.SURPRISED -> eyeW * 0.48f // wide circular
                emotion == AiEmotion.HAPPY -> eyeW * 0.38f
                else -> eyeW * 0.32f
            }

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

            // 2. Eyelid cuts
            val finalEyePath = if (emotion == AiEmotion.HAPPY) {
                val smileCut = Path().apply {
                    val smileH = effectiveH * 0.52f
                    moveTo(eyeLeft - 10f, eyeBottom + 10f)
                    quadraticTo(center.x, eyeBottom - smileH, eyeRight + 10f, eyeBottom + 10f)
                    lineTo(eyeRight + 10f, eyeBottom + 30f)
                    lineTo(eyeLeft - 10f, eyeBottom + 30f)
                    close()
                }
                val diff = Path()
                diff.op(eyePath, smileCut, PathOperation.Difference)
                diff
            } else if (browTilt != 0f) {
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

            // 3. Ambient Glow Border
            drawPath(
                path = finalEyePath,
                color = effectiveColor.copy(alpha = 0.35f + gazeState.audioAmplitude * 0.4f),
                style = Stroke(width = 8f)
            )

            // 4. Solid Neon Body
            drawPath(
                path = finalEyePath,
                brush = Brush.verticalGradient(
                    colors = listOf(effectiveColor, effectiveColor.copy(alpha = 0.85f)),
                    startY = eyeTop,
                    endY = eyeBottom
                )
            )

            // 5. Specular Inner Pupil Catchlight Tracking Gaze
            val maxShiftX = eyeW * 0.22f
            val maxShiftY = effectiveH * 0.22f
            val pupilX = center.x + (animatedGazeX * maxShiftX)
            val pupilY = center.y + (animatedGazeY * maxShiftY)

            val gleamRadius = (eyeW * 0.16f * pupilScale).coerceIn(4f, eyeW * 0.3f)
            val gleamOffset = Offset(
                pupilX + if (isLeft) -gleamRadius * 0.3f else gleamRadius * 0.3f,
                pupilY - gleamRadius * 0.35f
            )

            // Primary gleam
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = gleamRadius,
                center = gleamOffset
            )

            // Secondary sparkle
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = gleamRadius * 0.45f,
                center = Offset(
                    gleamOffset.x + (if (isLeft) gleamRadius * 0.9f else -gleamRadius * 0.9f),
                    gleamOffset.y + gleamRadius * 0.85f
                )
            )

            // Camera Tracking Active Indicator dot
            if (useCameraTracking && cameraGazeData?.isFaceDetected == true) {
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 3.5f,
                    center = Offset(eyeRight - 12f, eyeTop + 12f)
                )
            }
        }
    }
}

/**
 * Reusable GazeEyePair component displaying left and right eyes side-by-side
 * with synchronized camera gaze tracking and touch responsiveness.
 */
@Composable
fun GazeEyePair(
    modifier: Modifier = Modifier,
    gazeState: EyeGazeState = EyeGazeState(),
    cameraGazeData: CameraGazeData? = null,
    useCameraTracking: Boolean = true,
    color: Color = Color(0xFF00E5FF),
    emotion: AiEmotion = AiEmotion.CALM,
    spacing: Dp = 24.dp,
    eyeSize: Dp = 120.dp,
    onEyesTapped: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.testTag("gaze_eye_pair"),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GazeEye(
            isLeft = true,
            gazeState = gazeState,
            cameraGazeData = cameraGazeData,
            useCameraTracking = useCameraTracking,
            color = color,
            emotion = emotion,
            onEyeTapped = onEyesTapped,
            modifier = Modifier.size(eyeSize)
        )

        GazeEye(
            isLeft = false,
            gazeState = gazeState,
            cameraGazeData = cameraGazeData,
            useCameraTracking = useCameraTracking,
            color = color,
            emotion = emotion,
            onEyeTapped = onEyesTapped,
            modifier = Modifier.size(eyeSize)
        )
    }
}
