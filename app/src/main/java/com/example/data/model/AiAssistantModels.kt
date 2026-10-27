package com.example.data.model

import java.util.UUID

enum class AttachedMediaType {
    IMAGE,
    PDF,
    DOCUMENT
}

data class AttachedMedia(
    val uri: String,
    val type: AttachedMediaType,
    val displayName: String,
    val sizeFormatted: String = "Ready to analyze",
    val extractedOcrText: String? = null,
    val ocrLanguage: String? = null
)

data class AiAssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachment: AttachedMedia? = null,
    val isThinking: Boolean = false,
    val subjectTag: String? = null,
    val suggestedPrompts: List<String> = emptyList(),
    val isHelpful: Boolean? = null, // null: unrated, true: thumb up, false: thumb down
    val isStopped: Boolean = false,
    val reasoningText: String? = null,
    val thinkingDurationSeconds: Double? = null
)

data class AiPromptSuggestion(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val promptText: String,
    val requiresUpload: AttachedMediaType? = null
)

/**
 * Available AI Model Providers for TheStudent AI Study Assistant.
 */
enum class AiModelChoice(
    val id: String,
    val displayName: String,
    val providerBadge: String,
    val description: String,
    val supportsTools: Boolean = false
) {
    GLM_4_7_FLASH(
        id = "glm-4.7-flash",
        displayName = "GLM-4.7-Flash",
        providerBadge = "Z.AI Free",
        description = "Free, high-speed inference with native tool calling",
        supportsTools = true
    ),
    HUGGING_FACE(
        id = "the-student-ai",
        displayName = "Hugging Face Space",
        providerBadge = "HF Space",
        description = "TheStudent AI (GPT-OSS 20B with deep reasoning)",
        supportsTools = false
    ),
    GEMINI_FLASH(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        providerBadge = "Google",
        description = "Multimodal vision & document comprehension",
        supportsTools = false
    )
}
