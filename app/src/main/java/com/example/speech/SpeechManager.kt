package com.example.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var toneGenerator: ToneGenerator? = null
    val elevenLabsService = ElevenLabsVoiceService(context)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _isWakeWordListening = MutableStateFlow(true)
    val isWakeWordListening: StateFlow<Boolean> = _isWakeWordListening.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onWakeWordDetected: (() -> Unit)? = null
    var isVoiceEnabled: Boolean = true

    // User can override or provide ElevenLabs API key directly in app
    var customApiKey: String = ""

    val effectiveApiKey: String
        get() {
            if (customApiKey.isNotBlank()) return customApiKey.trim()
            return try {
                BuildConfig.ELEVENLABS_API_KEY
            } catch (_: Throwable) {
                ""
            }
        }

    val isElevenLabsActive: Boolean
        get() = effectiveApiKey.isNotBlank() && effectiveApiKey != "MY_ELEVENLABS_API_KEY"

    val voiceId: String = ElevenLabsVoiceService.VOICE_ID

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var elevenLabsJob: Job? = null

    init {
        initTts()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (_: Exception) {}
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                // Crisp friendly robotic pitch
                textToSpeech?.setPitch(1.18f)
                textToSpeech?.setSpeechRate(1.02f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _audioAmplitude.value = 0f
                        // If wake word is enabled, resume listening after speaking
                        if (_isWakeWordListening.value) {
                            scope.launch {
                                delay(400)
                                startListening()
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _audioAmplitude.value = 0f
                        if (_isWakeWordListening.value) {
                            scope.launch {
                                delay(400)
                                startListening()
                            }
                        }
                    }
                })
                isTtsInitialized = true
            }
        }
    }

    fun playWakeChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (_: Exception) {}
    }

    fun setWakeWordListening(active: Boolean) {
        _isWakeWordListening.value = active
        if (active && !_isListening.value && !_isSpeaking.value) {
            startListening()
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        stopListening()
        stopSpeaking()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialText.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                        if (_isWakeWordListening.value && !_isSpeaking.value) {
                            scope.launch {
                                delay(600)
                                startListening()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim()

                        if (!spokenText.isNullOrEmpty()) {
                            _partialText.value = spokenText
                            handleSpokenInput(spokenText)
                        } else if (_isWakeWordListening.value && !_isSpeaking.value) {
                            scope.launch {
                                delay(500)
                                startListening()
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { partial ->
                            _partialText.value = partial
                            val lower = partial.lowercase(Locale.ROOT)
                            if (lower.contains("anima") || lower.contains("hey anima")) {
                                playWakeChime()
                                onWakeWordDetected?.invoke()
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            _isListening.value = false
        }
    }

    private fun handleSpokenInput(spokenText: String) {
        val lower = spokenText.lowercase(Locale.ROOT)
        val hasWakeWord = lower.contains("anima") || lower.contains("hey anima") || lower.contains("hi anima") || lower.contains("ok anima")

        if (hasWakeWord) {
            playWakeChime()
            onWakeWordDetected?.invoke()

            val command = spokenText
                .replace(Regex("(?i)^(hey\\s+anima|hi\\s+anima|ok\\s+anima|anima)[,!\\s]*"), "")
                .trim()

            if (command.isNotEmpty()) {
                onSpeechRecognized?.invoke(command)
            } else {
                onSpeechRecognized?.invoke("Hello Anima")
            }
        } else {
            onSpeechRecognized?.invoke(spokenText)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _audioAmplitude.value = 0f
        }
    }

    fun speak(text: String) {
        if (!isVoiceEnabled) return

        stopSpeaking()
        val cleanSpeech = text
            .replace(Regex("[#*`_>~\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .take(350)

        // Try ElevenLabs if configured
        if (isElevenLabsActive) {
            elevenLabsJob = scope.launch {
                _isSpeaking.value = true
                _audioAmplitude.value = 0.3f

                val audioFile = elevenLabsService.synthesizeSpeech(cleanSpeech, effectiveApiKey)
                if (audioFile != null && audioFile.exists()) {
                    elevenLabsService.playAudio(
                        file = audioFile,
                        scope = scope,
                        onStart = {
                            _isSpeaking.value = true
                        },
                        onAmplitude = { amp ->
                            _audioAmplitude.value = amp
                        },
                        onDone = {
                            _isSpeaking.value = false
                            _audioAmplitude.value = 0f
                            if (_isWakeWordListening.value) {
                                scope.launch {
                                    delay(400)
                                    startListening()
                                }
                            }
                        }
                    )
                } else {
                    // Fallback to on-device TTS if network error or quota
                    speakWithSystemTts(cleanSpeech)
                }
            }
        } else {
            // Direct on-device System TTS
            speakWithSystemTts(cleanSpeech)
        }
    }

    private fun speakWithSystemTts(cleanSpeech: String) {
        if (!isTtsInitialized) return
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "anima_reply_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, "anima_reply")
    }

    fun stopSpeaking() {
        elevenLabsJob?.cancel()
        elevenLabsJob = null
        elevenLabsService.stopAudio()

        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
            }
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _audioAmplitude.value = 0f
    }

    fun release() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
