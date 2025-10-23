package com.joshbeth.thebet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// To manage navigation
enum class Screen {
    PLAYER_DESIGNATION,
    REWARD_PUNISHMENT_CHOICE,
    STORY_SELECTION,
    STORY_SCREEN,
    STORY_PLAYER // New screen for the act player
}

// To manage the app's visual theme
enum class ThemeSelection {
    NEUTRAL,
    PUNISHMENT,
    REWARD
}

@Serializable
data class StoryUiState(
    val currentScreen: Screen = Screen.PLAYER_DESIGNATION,
    val theme: ThemeSelection = ThemeSelection.NEUTRAL,
    val winnerName: String = "",
    val loserName: String = "",
    val chosenPath: String? = null, // "Reward" or "Punishment"
    val selectedStory: StoryScript? = null,
    val drawnCommands: Map<Int, StoryCommand?> = emptyMap(),
    val actToPlay: List<StoryStep>? = null // The act currently being played
)

@Serializable
data class StoryScript(
    val id: String,
    val title: String,
    val type: String, // "Reward" or "Punishment"
    val dominantGender: String? = null, // "Male", "Female", or null for unisex
    val imagePrompt: String,
    val imagePlaceholder: String,
    val concept: String,
    val commandLibrary: CommandLibrary,
    val act1Setup: List<StoryStep>,
    val act2Core: List<StoryStep>,
    val act3Aftermath: List<StoryStep>,
    val aftercareScript: List<StoryStep>? = null
)

@Serializable
data class CommandLibrary(
    val instruction: List<StoryCommand>? = null,
    val instructiona: List<StoryCommand>? = null,
    val humiliation: List<StoryCommand>? = null,
    val praise: List<StoryCommand>? = null,
    val toy_use: Map<String, List<StoryCommand>>? = null,
    val actionsDomOnSubHandsOnBody: List<StoryCommand>? = null,
    val actionsDomOnSubHandsOnPussy: List<StoryCommand>? = null,
    val actionsDomOnSubHandsOnCock: List<StoryCommand>? = null,
    val actionsSubOnDomMouthOnPussy: List<StoryCommand>? = null,
    val actionsSubOnDomMouthOnCock: List<StoryCommand>? = null,
    val subToDomWorship: List<StoryCommand>? = null,
    val kinkActions: Map<String, List<StoryCommand>>? = null,
    val aftercare: Map<String, List<StoryCommand>>? = null
)

@Serializable
data class StoryCommand(
    val text: String
)

@Serializable
sealed interface StoryStep

@Serializable
@SerialName("dialogue")
data class DialogueLine(val speaker: String, val text: String) : StoryStep

@Serializable
@SerialName("action")
data class ActionTag(val tag: String) : StoryStep

@Serializable
@SerialName("draw")
data class DrawCommand(val from: String) : StoryStep
