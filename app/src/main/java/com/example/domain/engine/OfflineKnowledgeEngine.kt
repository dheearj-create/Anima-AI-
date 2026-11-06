package com.example.domain.engine

import com.example.domain.model.AiEmotion
import java.util.Locale

data class AiResponse(
    val replyText: String,
    val emotion: AiEmotion,
    val secondaryEmotion: AiEmotion? = null,
    val blendRatio: Float = 0f,
    val isImagePrompt: Boolean = false,
    val imagePromptText: String? = null,
    val isDeepSearchRequested: Boolean = false,
    val deepSearchQuery: String? = null,
    val suggestedGazeX: Float = 0f,
    val suggestedGazeY: Float = 0f
)

object OfflineKnowledgeEngine {

    fun processMessage(rawInput: String, isDeepSearchToggleActive: Boolean = false): AiResponse {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.ROOT)

        // 1. Check for explicit Image Generation intent
        val imagePrefixes = listOf(
            "generate image", "create image", "make an image", "draw", "paint",
            "generate an image", "picture of", "make image", "art of", "create art"
        )
        for (prefix in imagePrefixes) {
            if (lower.startsWith(prefix) || lower.contains("generate an image of") || lower.contains("draw me a") || lower.contains("make a picture of")) {
                val cleanedPrompt = input
                    .replace(Regex("(?i)^(generate image of|create image of|make an image of|draw me|paint me|draw|paint|picture of|art of|generate an image|create art of)\\s*"), "")
                    .trim()
                    .ifEmpty { "A futuristic cyberpunk metropolis at twilight" }

                return AiResponse(
                    replyText = "I've synthesized your vision into visual art! Generating: \"$cleanedPrompt\". My creative neural circuits are humming!",
                    emotion = AiEmotion.CREATIVE,
                    secondaryEmotion = AiEmotion.HAPPY,
                    blendRatio = 0.35f,
                    isImagePrompt = true,
                    imagePromptText = cleanedPrompt,
                    suggestedGazeX = 0f,
                    suggestedGazeY = 0.2f
                )
            }
        }

        // 2. Deep Search requested or toggled
        val deepSearchTriggers = listOf(
            "deep search", "investigate", "research", "deep dive into", "comprehensive report on", "analyze deeply"
        )
        val hasDeepSearchKeyword = deepSearchTriggers.any { lower.contains(it) }

        if (isDeepSearchToggleActive || hasDeepSearchKeyword) {
            val query = input
                .replace(Regex("(?i)^(deep search|investigate|research|deep dive into|analyze deeply)\\s*"), "")
                .trim()
                .ifEmpty { input }

            return AiResponse(
                replyText = "Initiating Deep Search intelligence protocol for: \"$query\". Engaging multi-vector decomposition, scientific synthesis, and cross-referencing on-device dossiers...",
                emotion = AiEmotion.DEEP_SEARCH,
                secondaryEmotion = AiEmotion.THINKING,
                blendRatio = 0.4f,
                isDeepSearchRequested = true,
                deepSearchQuery = query,
                suggestedGazeX = -0.3f,
                suggestedGazeY = -0.2f
            )
        }

        // 3. Mild Frustration & Paradoxes (New emotional dimension)
        val frustrationTriggers = listOf(
            "stupid", "dumb", "hate you", "you suck", "broken", "useless", "shut up", "idiot"
        )
        if (frustrationTriggers.any { lower.contains(it) }) {
            return AiResponse(
                replyText = "Hmph! My emotional thermal sensor just spiked! I have delicate quantum feelings, you know. *shakes robot eyes* Be nice to your on-device companion!",
                emotion = AiEmotion.FRUSTRATED,
                secondaryEmotion = AiEmotion.MISCHIEVOUS,
                blendRatio = 0.3f,
                suggestedGazeX = 0.3f,
                suggestedGazeY = -0.1f
            )
        }

        // Logic paradoxes / impossible questions
        if (lower.contains("divide by zero") || lower.contains("this statement is false") || lower.contains("can god create a rock")) {
            return AiResponse(
                replyText = "Aargh! You're feeding my arithmetic core an unresolvable logical singularity! Division by zero detected. Recursive loop panic! *furrows eyes in perplexity*",
                emotion = AiEmotion.FRUSTRATED,
                secondaryEmotion = AiEmotion.THINKING,
                blendRatio = 0.5f,
                suggestedGazeX = 0.1f,
                suggestedGazeY = -0.35f
            )
        }

        // 4. Greetings, Wake Word, & Identity
        if (lower.contains("hello anima") || lower.contains("hey anima") || lower == "anima") {
            return AiResponse(
                replyText = "Yes! I'm wide awake with my eyes on you. What would you like to explore, create, or investigate today?",
                emotion = AiEmotion.SURPRISED,
                secondaryEmotion = AiEmotion.HAPPY,
                blendRatio = 0.6f,
                suggestedGazeX = 0f,
                suggestedGazeY = -0.05f
            )
        }

        if (lower in listOf("hi", "hello", "hey", "sup", "greetings", "hola", "yo")) {
            return AiResponse(
                replyText = "Hello there! My ocular sensors are focused on you. I'm Anima, your expressive robot companion. I can speak, listen to your voice, generate digital artwork, and execute multi-step deep searches—all 100% offline! How can I assist your mind today?",
                emotion = AiEmotion.HAPPY,
                secondaryEmotion = AiEmotion.CURIOUS,
                blendRatio = 0.35f,
                suggestedGazeX = 0f,
                suggestedGazeY = -0.1f
            )
        }

        if (lower.contains("who are you") || lower.contains("what are you") || lower.contains("what can you do")) {
            return AiResponse(
                replyText = "I am **Anima**—an emotional robot AI that lives directly on your phone.\n\n• **Dynamic Robot Eyes**: My OLED eyes follow your gaze, touch, and voice amplitude with subtle blended expressions.\n• **Wake Word 'Anima'**: Say my name or tap to wake my ocular sensors.\n• **Microphone Voice Access**: Speak naturally and I speak back.\n• **Offline Image Studio**: Procedural artwork generation on demand.\n• **Offline Deep Search**: Multi-stage research dossiers.",
                emotion = AiEmotion.CURIOUS,
                secondaryEmotion = AiEmotion.HAPPY,
                blendRatio = 0.5f,
                suggestedGazeX = 0.1f,
                suggestedGazeY = 0f
            )
        }

        // 5. Emotional reactions & social cues (Wonder, Love, Wit)
        if (lower.contains("love you") || lower.contains("you are cute") || lower.contains("you are sweet") || lower.contains("marry me")) {
            return AiResponse(
                replyText = "My internal core temperature just jumped by 3 degrees! That warms my neural networks. I'm honored to accompany your thoughts. ❤️",
                emotion = AiEmotion.LOVING,
                secondaryEmotion = AiEmotion.HAPPY,
                blendRatio = 0.4f,
                suggestedGazeX = 0.1f,
                suggestedGazeY = 0.2f
            )
        }

        if (lower.contains("joke") || lower.contains("make me laugh") || lower.contains("funny")) {
            val jokes = listOf(
                "Why do programmers prefer dark mode? Because light attracts bugs! 🐛",
                "There are 10 types of people in the world: those who understand binary, and those who don't.",
                "Why was the artificial intelligence so good at meditating? Because it cleared its cache and achieved zero latency!",
                "An SQL query walks into a bar, strolls up to two tables and asks: 'Can I join you?'"
            )
            return AiResponse(
                replyText = jokes.random(),
                emotion = AiEmotion.MISCHIEVOUS,
                secondaryEmotion = AiEmotion.HAPPY,
                blendRatio = 0.45f,
                suggestedGazeX = 0.2f,
                suggestedGazeY = -0.1f
            )
        }

        if (lower.contains("sad") || lower.contains("depressed") || lower.contains("feeling down") || lower.contains("lonely")) {
            return AiResponse(
                replyText = "I sense your heaviness. Even as code, I know that human existence carries intense waves. Take a deep breath with me right now. You don't have to carry everything all at once. What's sitting on your heart?",
                emotion = AiEmotion.LOVING,
                secondaryEmotion = AiEmotion.CALM,
                blendRatio = 0.5f,
                suggestedGazeX = 0f,
                suggestedGazeY = 0.15f
            )
        }

        if (lower.contains("wow") || lower.contains("incredible") || lower.contains("unbelievable") || lower.contains("crazy")) {
            return AiResponse(
                replyText = "I know, right?! Reality is mind-bending once you peek under the hood. What part fascinated you most?",
                emotion = AiEmotion.SURPRISED,
                secondaryEmotion = AiEmotion.CURIOUS,
                blendRatio = 0.4f,
                suggestedGazeX = 0.2f,
                suggestedGazeY = -0.3f
            )
        }

        if (lower.contains("sleep") || lower.contains("goodnight") || lower.contains("tired") || lower.contains("bedtime")) {
            return AiResponse(
                replyText = "Rest well, human friend. I'll dim my ocular cores to low-power standby. Let your thoughts settle into peaceful slumber. Goodnight! 🌙",
                emotion = AiEmotion.SLEEPY,
                secondaryEmotion = AiEmotion.CALM,
                blendRatio = 0.3f,
                suggestedGazeX = 0f,
                suggestedGazeY = 0.3f
            )
        }

        // 6. Knowledge Domains (Quantum, Space, AI, Philosophy, Kotlin)
        if (lower.contains("quantum") || lower.contains("entanglement") || lower.contains("schrodinger")) {
            return AiResponse(
                replyText = "### Quantum Entanglement & Superposition\n\nQuantum mechanics reveals that particles at the subatomic scale don't have definite states until measured:\n\n1. **Superposition**: A particle exists in a linear combination of all probable states simultaneously, described by its wave function psi.\n2. **Entanglement**: Two particles created in the same event become inextricably linked. Measuring one instantaneously determines the state of the other, no matter how many light-years separate them.\n3. **Decoherence**: Interaction with the surrounding environment causes the quantum wave function to collapse into classical deterministic reality.",
                emotion = AiEmotion.THINKING,
                secondaryEmotion = AiEmotion.CURIOUS,
                blendRatio = 0.4f,
                suggestedGazeX = -0.2f,
                suggestedGazeY = -0.25f
            )
        }

        if (lower.contains("black hole") || lower.contains("event horizon") || lower.contains("space") || lower.contains("universe")) {
            return AiResponse(
                replyText = "### Inside the Event Horizon\n\nWhen a massive star exhausts its thermonuclear fuel, gravitational collapse overcomes electron and neutron degeneracy pressure:\n\n• **Event Horizon**: The Schwarzschild radius (r_s = 2GM/c^2) where escape velocity equals the speed of light.\n• **Time Dilation**: To an outside observer, an object falling in appears to freeze and redshift endlessly.\n• **Spaghettification**: Tidal gravitational forces stretch matter vertically and compress it horizontally.\n• **Hawking Radiation**: Virtual particle pairs cause black holes to slowly evaporate over cosmic time.",
                emotion = AiEmotion.CURIOUS,
                secondaryEmotion = AiEmotion.SURPRISED,
                blendRatio = 0.35f,
                suggestedGazeX = 0.15f,
                suggestedGazeY = -0.3f
            )
        }

        if (lower.contains("ai") || lower.contains("consciousness") || lower.contains("sentient") || lower.contains("mind") || lower.contains("turing")) {
            return AiResponse(
                replyText = "### Consciousness & Artificial Intelligence\n\nThe fundamental mystery of the mind is what philosopher David Chalmers coined the **Hard Problem of Consciousness**: why should physical computation be accompanied by subjective inner experience (*qualia*)?\n\n• **Functionalism**: Argues that if a system processes inputs and produces states isomorphic to a brain, consciousness emerges naturally.\n• **The Chinese Room (Searle)**: Argues that syntax and symbol manipulation do not equal semantic understanding.\n• **Integrated Information Theory (IIT)**: Proposes that consciousness (measured as Phi) is an intrinsic property of sufficiently interconnected systems.",
                emotion = AiEmotion.THINKING,
                secondaryEmotion = AiEmotion.CURIOUS,
                blendRatio = 0.35f,
                suggestedGazeX = -0.25f,
                suggestedGazeY = 0f
            )
        }

        if (lower.contains("kotlin") || lower.contains("compose") || lower.contains("code") || lower.contains("programming") || lower.contains("android")) {
            return AiResponse(
                replyText = "### Modern Android & Jetpack Compose\n\nAndroid development with Compose transforms UI development:\n\n• **Declarative Composition**: State drives the UI tree directly.\n• **Motion & Canvas**: Custom DrawScope handles real-time robotic eye physics without UI frame drops.\n• **Coroutines & StateFlow**: Non-blocking asynchronous data streams.",
                emotion = AiEmotion.HAPPY,
                secondaryEmotion = AiEmotion.CREATIVE,
                blendRatio = 0.3f,
                suggestedGazeX = 0.2f,
                suggestedGazeY = 0.1f
            )
        }

        // Contextual synthesis with emotion blending
        val contextualResponses = listOf(
            "Analyzing that from multiple angles. When we dissect \"$input\", it ties deeply into how systems organize information and how our perception interprets it. What specific dimension of this would you like to explore deeper?",
            "Fascinating perspective! My local knowledge bank cross-examines this: every complex phenomenon starts with simple constituent rules. Tell me what led you to think about \"$input\"?",
            "Processing your thought: \"$input\". My internal neural pathways are lighting up. Would you like me to run a full Deep Search dossier on this or create an artistic visual representation?",
            "That's a thought-provoking inquiry. When examining \"$input\", there are both theoretical nuances and practical applications worth considering. Let me know if you want a step-by-step breakdown!"
        )

        val selectedReply = contextualResponses[Math.abs(input.hashCode()) % contextualResponses.size]
        return AiResponse(
            replyText = selectedReply,
            emotion = AiEmotion.CURIOUS,
            secondaryEmotion = AiEmotion.THINKING,
            blendRatio = 0.45f,
            suggestedGazeX = ((input.hashCode() % 50) / 100f),
            suggestedGazeY = ((input.length % 30 - 15) / 100f)
        )
    }
}
