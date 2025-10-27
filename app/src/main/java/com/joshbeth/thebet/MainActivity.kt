package com.joshbeth.thebet

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshbeth.thebet.ui.theme.TheBetTheme
import com.joshbeth.thebet.ui.theme.DeepBlack
import com.joshbeth.thebet.ui.theme.CrimsonRed
import com.joshbeth.thebet.ui.theme.DeepTeal
import com.joshbeth.thebet.ui.theme.DialogueGreen
import com.joshbeth.thebet.ui.theme.DominantBlue
import com.joshbeth.thebet.ui.theme.NeutralGrayDark
import com.joshbeth.thebet.ui.theme.NeutralGrayMedium
import com.joshbeth.thebet.ui.theme.SexyPeach
import com.joshbeth.thebet.ui.theme.SexyPink
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

// =============================================================== //
// VIEWMODEL
// =============================================================== //

class StoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    fun goBackToStart() {
        _uiState.update { StoryUiState() }
    }

    fun setPlayerNames(winner: String, loser: String) {
        _uiState.update {
            it.copy(
                winnerName = winner,
                loserName = loser,
                currentScreen = Screen.REWARD_PUNISHMENT_CHOICE
            )
        }
    }

    fun setChoice(path: String) {
        val theme = if (path == "Punishment") ThemeSelection.PUNISHMENT else ThemeSelection.REWARD
        _uiState.update { it.copy(
            chosenPath = path,
            currentScreen = Screen.STORY_SELECTION,
            theme = theme
        ) }
    }

    fun startStory(story: StoryScript, curatedCommands: Map<Int, StoryCommand>? = null) {
        viewModelScope.launch {
            val drawnCommands = curatedCommands?.toMutableMap() ?: mutableMapOf()
            val allSteps = story.act1Setup + story.act2Core + story.act3Aftermath + (story.aftercareScript ?: emptyList())

            val acts = listOf(story.act1Setup, story.act2Core, story.act3Aftermath, (story.aftercareScript ?: emptyList()))
            val actOffsets = listOf(0, story.act1Setup.size, story.act1Setup.size + story.act2Core.size, allSteps.size - (story.aftercareScript?.size ?: 0))

            acts.forEachIndexed { actIndex, act ->
                val actStartOffset = actOffsets[actIndex]
                val actEndOffset = actStartOffset + act.size
                val usedCommandsInAct = drawnCommands
                    .filter { (index, _) -> index >= actStartOffset && index < actEndOffset }
                    .values
                    .mapNotNull { it }
                    .toMutableSet()

                act.forEachIndexed { stepIndexInAct, step ->
                    if (step is DrawCommand) {
                        val globalIndex = actStartOffset + stepIndexInAct
                        if (!drawnCommands.containsKey(globalIndex)) {
                            val drawnCommand = CommandRepository.getRandomCommand(step.from, story.commandLibrary, usedCommandsInAct)
                            if (drawnCommand != null) {
                                usedCommandsInAct.add(drawnCommand)
                                drawnCommands[globalIndex] = drawnCommand
                            }
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    selectedStory = story,
                    currentScreen = Screen.STORY_SCREEN,
                    drawnCommands = drawnCommands
                )
            }
        }
    }

    fun playAct(act: List<StoryStep>) {
        _uiState.update {
            it.copy(
                actToPlay = act,
                currentScreen = Screen.STORY_PLAYER
            )
        }
    }

    fun startCuration(story: StoryScript) {
        _uiState.update {
            it.copy(
                selectedStory = story,
                currentScreen = Screen.COMMAND_CURATION
            )
        }
    }

    fun finishAct() {
        _uiState.update {
            it.copy(
                actToPlay = null,
                currentScreen = Screen.STORY_SCREEN
            )
        }
    }
}

// =============================================================== //
// ACTIVITY
// =============================================================== //

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: StoryViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                StoryRepository.loadStories(applicationContext)
            }

            TheBetTheme(theme = uiState.theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StoryApp(viewModel = viewModel)
                }
            }
        }
    }
}

// =============================================================== //
// COMPOSABLE SCREENS
// =============================================================== //

@Composable
fun GradientBox(theme: ThemeSelection, content: @Composable () -> Unit) {
    val gradientColors = when (theme) {
        ThemeSelection.PUNISHMENT -> listOf(DeepBlack, CrimsonRed.copy(alpha = 0.3f))
        ThemeSelection.REWARD -> listOf(DeepTeal, SexyPeach.copy(alpha = 0.3f))
        ThemeSelection.NEUTRAL -> listOf(NeutralGrayDark, NeutralGrayMedium)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = gradientColors)
            )
    ) {
        content()
    }
}

@Composable
fun StoryApp(modifier: Modifier = Modifier, viewModel: StoryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    GradientBox(theme = uiState.theme) {
        when (uiState.currentScreen) {
            Screen.PLAYER_DESIGNATION -> PlayerDesignationScreen(onConfirm = { winner, loser ->
                viewModel.setPlayerNames(winner, loser)
            })
            Screen.REWARD_PUNISHMENT_CHOICE -> RewardPunishmentChoiceScreen(
                winnerName = uiState.winnerName,
                onChoiceSelected = { choice -> viewModel.setChoice(choice) }
            )
            Screen.STORY_SELECTION -> StorySelectionScreen(
                winnerName = uiState.winnerName,
                loserName = uiState.loserName,
                path = uiState.chosenPath ?: "",
                onStorySelected = { story -> viewModel.startStory(story) },
                onCurationSelected = { story -> viewModel.startCuration(story) },
                onBack = { viewModel.goBackToStart() }
            )
            Screen.STORY_SCREEN -> StoryScreen(uiState = uiState, onPlayAct = { act -> viewModel.playAct(act) }, onBack = { viewModel.goBackToStart() })
            Screen.STORY_PLAYER -> StoryPlayerScreen(uiState = uiState, onFinish = { viewModel.finishAct() })
            Screen.COMMAND_CURATION -> CommandCurationScreen(uiState = uiState, onConfirm = { story, commands -> viewModel.startStory(story, commands) }, onBack = { viewModel.goBackToStart() })
        }
    }
}

@Composable
fun PlayerDesignationScreen(onConfirm: (String, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Who Won The Bet?", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onConfirm("Josh", "Beth") },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Josh Won", color = MaterialTheme.colorScheme.onPrimary)
            }
            Button(
                onClick = { onConfirm("Beth", "Josh") },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Beth Won", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun RewardPunishmentChoiceScreen(winnerName: String, onChoiceSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Congratulations, $winnerName!", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "What is your desire?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Button(
                onClick = { onChoiceSelected("Reward") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Claim a Reward", color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(
                onClick = { onChoiceSelected("Punishment") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Deal a Punishment", color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}


@Composable
fun StorySelectionScreen(
    winnerName: String,
    loserName: String,
    path: String,
    onStorySelected: (StoryScript) -> Unit,
    onCurationSelected: (StoryScript) -> Unit,
    onBack: () -> Unit
) {
    var selectedStoryId by remember { mutableStateOf<String?>(null) }

    val stories = StoryRepository.getAllStories()

    if (selectedStoryId != null) {
        val story = stories.find { it.id == selectedStoryId }!!
        AlertDialog(
            onDismissRequest = { selectedStoryId = null },
            title = { Text("Choose Mode") },
            text = { Text("How do you want to experience this story?") },
            confirmButton = {
                Button(onClick = {
                    onStorySelected(story)
                    selectedStoryId = null
                }) {
                    Text("Random")
                }
            },
            dismissButton = {
                Button(onClick = {
                    onCurationSelected(story)
                    selectedStoryId = null
                }) {
                    Text("Curated")
                }
            }
        )
    }
    // For Rewards, the dominant gender is the winner.
    // For Punishments, the 'dominantGender' in the JSON refers to the person being punished (the loser).
    val targetGender = if (path.equals("Punishment", ignoreCase = true)) {
        if (loserName.equals("Josh", ignoreCase = true)) "Male" else "Female"
    } else { // Reward
        if (winnerName.equals("Josh", ignoreCase = true)) "Male" else "Female"
    }

    val filteredStories = stories.filter {
        it.type.equals(path, ignoreCase = true) && (it.dominantGender == null || it.dominantGender.equals(targetGender, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    text = "Choose a $path",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            if (filteredStories.isEmpty()) {
                item {
                    Text(
                        "No stories available for this selection.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                items(filteredStories) { story ->
                    val isPlayable = story.id == "female_reward_01_audio_ref" || story.id == "male_punishment_01" || story.id == "male_reward_01" || story.id == "female_punishment_01"
                    val cardAlpha = if (isPlayable) 1f else 0.5f

                    Card(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .alpha(cardAlpha)
                            .clickable(enabled = isPlayable) { selectedStoryId = story.id },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val conceptText = story.concept
                                    .replace("{winner}", winnerName, ignoreCase = true)
                                    .replace("{loser}", loserName, ignoreCase = true)
                                Text(text = story.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = conceptText, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!isPlayable) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Coming Soon",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back", color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}

@Composable
fun CommandCurationScreen(uiState: StoryUiState, onConfirm: (StoryScript, Map<Int, StoryCommand>) -> Unit, onBack: () -> Unit) {
    val story = uiState.selectedStory ?: return
    val allSteps = story.act1Setup + story.act2Core + story.act3Aftermath + (story.aftercareScript ?: emptyList())
    val commandSteps = allSteps.mapIndexedNotNull { index, step ->
        if (step is DrawCommand) index to step else null
    }

    var selectedCommands by remember { mutableStateOf<Map<Int, StoryCommand>>(emptyMap()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Curate Commands",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = story.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(commandSteps) { (index, step) ->
                var expanded by remember { mutableStateOf(false) }
                val selectedCommand = selectedCommands[index]
                val options = CommandRepository.getCommandsForCategory(step.from, story.commandLibrary)

                Column {
                    Text(
                        text = "Draw from: ${step.from}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.clickable { expanded = true }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (selectedCommand != null) {
                                    Text(
                                        text = selectedCommand.text,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1
                                    )
                                } else {
                                    Text(
                                        text = "Randomly Selected",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Open options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Randomly Select", fontStyle = FontStyle.Italic) },
                                    onClick = {
                                        selectedCommands = selectedCommands - index
                                        expanded = false
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                options.forEachIndexed { itemIndex, command ->
                                    DropdownMenuItem(
                                        text = { Text(command.text) },
                                        onClick = {
                                            selectedCommands = selectedCommands + (index to command)
                                            expanded = false
                                        }
                                    )
                                    if (itemIndex < options.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.padding(top = 16.dp)) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back", color = MaterialTheme.colorScheme.onSecondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { onConfirm(story, selectedCommands) },
                enabled = true,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Story", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun StoryScreen(uiState: StoryUiState, onPlayAct: (List<StoryStep>) -> Unit, onBack: () -> Unit) {
    val story = uiState.selectedStory ?: return
    val allSteps = story.act1Setup + story.act2Core + story.act3Aftermath + (story.aftercareScript ?: emptyList())

    val actSections = listOfNotNull(
        "Act 1: Setup" to story.act1Setup,
        "Act 2: Core" to story.act2Core,
        "Act 3: Aftermath" to story.act3Aftermath,
        if (story.aftercareScript?.isNotEmpty() == true) "Aftercare" to story.aftercareScript else null
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            items(actSections) { (title, actContent) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Button(
                                onClick = { onPlayAct(actContent) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Act", tint = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.secondary)

                        actContent.forEach { step ->
                            val stepIndex = allSteps.indexOf(step)
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                when (step) {
                                    is DialogueLine -> {
                                        val textToDisplay = step.text.replace("{winner}", uiState.winnerName, true).replace("{loser}", uiState.loserName, true)
                                        val speakerToDisplay = when (step.speaker.uppercase()) {
                                            "WINNER" -> uiState.winnerName
                                            "LOSER" -> uiState.loserName
                                            else -> step.speaker
                                        }
                                        val dialogueColor = when (speakerToDisplay.uppercase()) {
                                            "BETH" -> SexyPink
                                            "JOSH" -> DominantBlue
                                            "NARRATOR" -> DialogueGreen
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                        Text(text = "$speakerToDisplay: $textToDisplay", style = MaterialTheme.typography.bodyLarge, color = dialogueColor)
                                    }
                                    is ActionTag -> {
                                        Text(
                                            text = step.tag,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    is DrawCommand -> {
                                        val command = uiState.drawnCommands[stepIndex]
                                        if (command != null) {
                                            val rawText = command.text
                                            val lastBracketContent = rawText.substringAfterLast('[', "").substringBeforeLast(']', "")

                                            val parsedSpeaker: String
                                            val parsedDialogue: String
                                            if (lastBracketContent.contains(':')) {
                                                val parts = lastBracketContent.split(":", limit = 2)
                                                parsedSpeaker = parts[0].trim()
                                                parsedDialogue = parts[1].trim().removeSurrounding("'")
                                            } else {
                                                parsedSpeaker = uiState.winnerName
                                                parsedDialogue = rawText.trim('[', ']')
                                            }

                                            val commandText = parsedDialogue.replace("{winner}", uiState.winnerName, true).replace("{loser}", uiState.loserName, true)
                                            val speakerToDisplay = when (parsedSpeaker.uppercase()) {
                                                "WINNER" -> uiState.winnerName
                                                "LOSER" -> uiState.loserName
                                                else -> parsedSpeaker
                                            }
                                            val dialogueColor = when (speakerToDisplay.uppercase()) {
                                                "BETH" -> SexyPink
                                                "JOSH" -> DominantBlue
                                                "NARRATOR" -> DialogueGreen
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = "Draw Command",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "$speakerToDisplay: $commandText",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = dialogueColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Start", color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}

// =============================================================== //
// AUDIO HELPERS
// =============================================================== //

fun assetExists(context: Context, path: String): Boolean {
    return try {
        context.assets.open(path).close()
        true
    } catch (e: IOException) {
        false
    }
}

fun findCommandAudioFilename(storyId: String, command: StoryCommand, library: CommandLibrary): String? {
    // Check simple lists
    library.instruction?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_instruction_${index}.mp3"
    }
    library.instructiona?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_instructiona_${index}.mp3"
    }
    library.instruction_climax?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_instruction_climax_${index}.mp3"
    }
    library.humiliation?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_humiliation_${index}.mp3"
    }
    library.praise?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_praise_${index}.mp3"
    }
    library.actionsDomOnSubHandsOnBody?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_actionsDomOnSubHandsOnBody_${index}.mp3"
    }
    library.actionsDomOnSubHandsOnPussy?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_actionsDomOnSubHandsOnPussy_${index}.mp3"
    }
    library.actionsDomOnSubHandsOnCock?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_actionsDomOnSubHandsOnCock_${index}.mp3"
    }
    library.actionsSubOnDomMouthOnPussy?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_actionsSubOnDomMouthOnPussy_${index}.mp3"
    }
    library.actionsSubOnDomMouthOnCock?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_actionsSubOnDomMouthOnCock_${index}.mp3"
    }
    library.subToDomWorship?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_subToDomWorship_${index}.mp3"
    }
    library.position?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_position_${index}.mp3"
    }
    library.intensity?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_intensity_${index}.mp3"
    }
    library.setup_humiliation?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_setup_humiliation_${index}.mp3"
    }
    library.positioning?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_positioning_${index}.mp3"
    }
    library.climax_instruction?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_climax_instruction_${index}.mp3"
    }
    library.instruction_force_position?.let { list ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_instruction_force_position_${index}.mp3"
    }

    // Check map-based lists
    library.toy_use?.forEach { (subKey, list) ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_toy_use_${subKey.replace(" ", "_")}_${index}.mp3"
    }
    library.kinkActions?.forEach { (subKey, list) ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_kinkActions_${subKey.replace(" ", "_")}_${index}.mp3"
    }
    library.aftercare?.forEach { (subKey, list) ->
        val index = list.indexOf(command)
        if (index != -1) return "${storyId}_lib_aftercare_${subKey.replace(" ", "_")}_${index}.mp3"
    }

    return null // Command not found
}


@Composable
fun StoryPlayerScreen(uiState: StoryUiState, onFinish: () -> Unit) {
    val context = LocalContext.current
    val act = uiState.actToPlay ?: return
    val story = uiState.selectedStory ?: return
    val allSteps = story.act1Setup + story.act2Core + story.act3Aftermath + (story.aftercareScript ?: emptyList())

    val playableSteps = act.filter { it is DialogueLine || it is DrawCommand }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    if (playableSteps.isEmpty()) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    val alpha = remember { Animatable(0f) }
    val currentLineIndex = remember { Animatable(0f) }

    //This effect will handle the audio for the current line.
    LaunchedEffect(currentLineIndex.value) {
        mediaPlayer?.release()
        mediaPlayer = null

        val index = currentLineIndex.value.toInt()
        if (index >= playableSteps.size) return@LaunchedEffect

        val step = playableSteps[index]

        // Construct the filename
        val audioFileName: String? = when (step) {
            is DialogueLine -> {
                val actName = when (act) {
                    story.act1Setup -> "act1Setup"
                    story.act2Core -> "act2Core"
                    story.act3Aftermath -> "act3Aftermath"
                    story.aftercareScript -> "aftercareScript"
                    else -> "unknownAct"
                }
                val speaker = step.speaker.replace(" ", "_")
                val localStepIndex = act.indexOf(step)
                "${story.id}_${actName}_${localStepIndex}_$speaker.mp3"
            }
            is DrawCommand -> {
                val stepIndexInStory = allSteps.indexOf(step)
                val command = uiState.drawnCommands[stepIndexInStory]
                if (command != null) {
                    findCommandAudioFilename(story.id, command, story.commandLibrary)
                } else null
            }
            else -> null
        }

        val assetPath = if (audioFileName != null) "audio/$audioFileName" else null
        if (assetPath != null && assetExists(context, assetPath)) {
            try {
                val afd = context.assets.openFd(assetPath)
                val mp = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    prepare()
                    start()
                    setOnCompletionListener { it.release() }
                }
                mediaPlayer = mp
                afd.close()
            } catch (e: Exception) {
                mediaPlayer = null
            }
        }
    }


    // This effect handles the text animation sequence
    LaunchedEffect(playableSteps) {
        while (currentLineIndex.value.toInt() < playableSteps.size) {
            alpha.animateTo(1f, animationSpec = tween(1000))

            val step = playableSteps[currentLineIndex.value.toInt()]
            val displayTime = when (step) {
                is DialogueLine -> (step.text.split(" ").size * 300L).coerceAtLeast(2000L)
                is DrawCommand -> {
                    val stepIndexInStory = allSteps.indexOf(step)
                    val command = uiState.drawnCommands[stepIndexInStory]
                    val rawText = command?.text ?: ""
                    val lastBracketContent = rawText.substringAfterLast('[', "").substringBeforeLast(']', "")
                    val dialogue = if (lastBracketContent.contains(':')) {
                        lastBracketContent.split(":", limit = 2)[1].trim().removeSurrounding("'")
                    } else {
                        rawText.trim('[', ']')
                    }
                    (dialogue.split(" ").size * 300L).coerceAtLeast(2000L)
                }
                else -> 2000L
            }
            delay(displayTime)

            alpha.animateTo(0f, animationSpec = tween(1000))
            currentLineIndex.animateTo(currentLineIndex.value + 1, animationSpec = tween(0))

            if (currentLineIndex.value.toInt() >= playableSteps.size) {
                onFinish()
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { 
            mediaPlayer?.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp).clickable { onFinish() },
        contentAlignment = Alignment.Center
    ) {
        if (currentLineIndex.value.toInt() >= playableSteps.size) return@Box

        val currentIndex = currentLineIndex.value.toInt()
        val step = playableSteps[currentIndex]

        when (step) {
            is DialogueLine -> {
                val textToDisplay = step.text.replace("{winner}", uiState.winnerName, true).replace("{loser}", uiState.loserName, true)
                val speakerToDisplay = when (step.speaker.uppercase()) {
                    "WINNER" -> uiState.winnerName
                    "LOSER" -> uiState.loserName
                    else -> step.speaker
                }
                val dialogueColor = when (speakerToDisplay.uppercase()) {
                    "BETH" -> SexyPink
                    "JOSH" -> DominantBlue
                    "NARRATOR" -> DialogueGreen
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val glowRadius = if (speakerToDisplay.equals("Josh", ignoreCase = true)) 32f else 16f
                val glowStyle = MaterialTheme.typography.headlineLarge.copy(shadow = Shadow(color = dialogueColor, blurRadius = glowRadius))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.alpha(alpha.value)) {
                    // Border/stroke text
                    Text(
                        text = "$speakerToDisplay: $textToDisplay",
                        style = glowStyle.copy(
                            drawStyle = Stroke(width = 4.0f)
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    // Fill text
                    Text(
                        text = "$speakerToDisplay: $textToDisplay",
                        style = glowStyle,
                        color = dialogueColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is DrawCommand -> {
                val stepIndex = allSteps.indexOf(step)
                val command = uiState.drawnCommands[stepIndex]
                if (command != null) {
                    val rawText = command.text
                    val lastBracketContent = rawText.substringAfterLast('[', "").substringBeforeLast(']', "")

                    val parsedSpeaker: String
                    val parsedDialogue: String
                    if (lastBracketContent.contains(':')) {
                        val parts = lastBracketContent.split(":", limit = 2)
                        parsedSpeaker = parts[0].trim()
                        parsedDialogue = parts[1].trim().removeSurrounding("'")
                    } else {
                        parsedSpeaker = uiState.winnerName
                        parsedDialogue = rawText.trim('[', ']')
                    }

                    val commandText = parsedDialogue.replace("{winner}", uiState.winnerName, true).replace("{loser}", uiState.loserName, true)
                    val speakerToDisplay = when (parsedSpeaker.uppercase()) {
                        "WINNER" -> uiState.winnerName
                        "LOSER" -> uiState.loserName
                        else -> parsedSpeaker
                    }
                    val dialogueColor = when (speakerToDisplay.uppercase()) {
                        "BETH" -> SexyPink
                        "JOSH" -> DominantBlue
                        "NARRATOR" -> DialogueGreen
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val glowRadius = if (speakerToDisplay.equals("Josh", ignoreCase = true)) 32f else 16f
                    val glowStyle = MaterialTheme.typography.headlineLarge.copy(shadow = Shadow(color = dialogueColor, blurRadius = glowRadius))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.alpha(alpha.value)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Draw Command",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(contentAlignment = Alignment.Center) {
                            // Border/stroke text
                            Text(
                                text = "$speakerToDisplay: $commandText",
                                style = glowStyle.copy(
                                    drawStyle = Stroke(width = 6.0f)
                                ),
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            // Fill text
                            Text(
                                text = "$speakerToDisplay: $commandText",
                                style = glowStyle,
                                color = dialogueColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
