package com.example

import com.example.domain.engine.OfflineKnowledgeEngine
import com.example.domain.model.AiEmotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimaEngineTest {

  @Test
  fun testOfflineKnowledgeEngineImagePrompt() {
    val response = OfflineKnowledgeEngine.processMessage("generate image of neon cyber cat")
    assertTrue(response.isImagePrompt)
    assertEquals(AiEmotion.CREATIVE, response.emotion)
  }

  @Test
  fun testOfflineKnowledgeEngineDeepSearchIntent() {
    val response = OfflineKnowledgeEngine.processMessage("deep search quantum mechanics")
    assertTrue(response.isDeepSearchRequested)
    assertEquals(AiEmotion.DEEP_SEARCH, response.emotion)
  }

  @Test
  fun testOfflineKnowledgeEngineGreetings() {
    val response = OfflineKnowledgeEngine.processMessage("hello")
    assertEquals(AiEmotion.HAPPY, response.emotion)
    assertTrue(response.replyText.contains("Anima"))
  }

  @Test
  fun testOfflineKnowledgeEngineFrustration() {
    val response = OfflineKnowledgeEngine.processMessage("stupid broken robot")
    assertEquals(AiEmotion.FRUSTRATED, response.emotion)
    assertNotNull(response.secondaryEmotion)
    assertTrue(response.blendRatio > 0f)
  }

  @Test
  fun testOfflineKnowledgeEngineWakeWord() {
    val response = OfflineKnowledgeEngine.processMessage("hey anima")
    assertEquals(AiEmotion.SURPRISED, response.emotion)
    assertEquals(AiEmotion.HAPPY, response.secondaryEmotion)
  }

  @Test
  fun testCameraGazeDataClamping() {
    val gazeData = com.example.camera.CameraGazeData(
      gazeX = 0.45f,
      gazeY = -0.32f,
      isFaceDetected = true,
      faceConfidence = 0.95f,
      isTrackingActive = true
    )
    assertTrue(gazeData.isFaceDetected)
    assertTrue(gazeData.isTrackingActive)
    assertTrue(gazeData.gazeX in -1f..1f)
    assertTrue(gazeData.gazeY in -1f..1f)
  }
}
