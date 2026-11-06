package com.example.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sin

class ElevenLabsVoiceService(private val context: Context) {

    companion object {
        const val VOICE_ID = "MzqUf1HbJ8UmQ0wUsx2p"
        private const val API_URL = "https://api.elevenlabs.io/v1/text-to-speech/$VOICE_ID"
        private const val TAG = "ElevenLabsVoiceService"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null

    suspend fun synthesizeSpeech(text: String, apiKey: String): File? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_ELEVENLABS_API_KEY") {
            return@withContext null
        }

        try {
            val json = JSONObject().apply {
                put("text", text.take(500))
                put("model_id", "eleven_turbo_v2_5")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.8)
                    put("style", 0.0)
                    put("use_speaker_boost", true)
                })
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "ElevenLabs API error: code=${response.code} body=${response.body?.string()}")
                    return@withContext null
                }

                val body = response.body ?: return@withContext null
                val outputFile = File(context.cacheDir, "elevenlabs_temp_voice.mp3")

                FileOutputStream(outputFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }

                outputFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synthesize ElevenLabs speech", e)
            null
        }
    }

    fun playAudio(
        file: File,
        scope: CoroutineScope,
        onStart: () -> Unit,
        onAmplitude: (Float) -> Unit,
        onDone: () -> Unit
    ) {
        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    onStart()

                    // Pulse amplitude while playing to animate robot eyes and mouth
                    amplitudeJob = scope.launch(Dispatchers.Default) {
                        var tick = 0f
                        while (isActive && mp.isPlaying) {
                            tick += 0.35f
                            // Generate natural rhythmic vocal cadence wave (0.25 to 0.95)
                            val wave = ((sin(tick.toDouble()) * 0.4 + 0.55) * (0.6 + (tick.toInt() % 3) * 0.2)).toFloat().coerceIn(0.1f, 1f)
                            onAmplitude(wave)
                            delay(60)
                        }
                        onAmplitude(0f)
                    }
                }
                setOnCompletionListener {
                    stopAudio()
                    onDone()
                }
                setOnErrorListener { _, _, _ ->
                    stopAudio()
                    onDone()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file", e)
            stopAudio()
            onDone()
        }
    }

    fun stopAudio() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
