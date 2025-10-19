package com.joshbeth.thebet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshbeth.thebet.ui.theme.TheBetTheme
import com.joshbeth.thebet.ui.theme.DeepPurple
import com.joshbeth.thebet.ui.theme.LightPink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
        _uiState.update { it.copy(chosenPath = path, currentScreen = Screen.STORY_SELECTION) }
    }

    fun startStory(story: StoryScript) {
        val allSteps = story.act1Setup + story.act2Core + story.act3Aftermath + (story.aftercareScript ?: emptyList())
        val drawnCommands = mutableMapOf<Int, StoryCommand?>()
        allSteps.forEachIndexed { index, step ->
            if (step is DrawCommand) {
                drawnCommands[index] = CommandRepository.getRandomCommand(step.from, story.commandLibrary)
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

// =============================================================== //
// ACTIVITY
// =============================================================== //

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StoryRepository.loadStories(applicationContext)
        setContent {
            TheBetTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StoryApp()
                }
            }
        }
    }
}

// =============================================================== //
// COMPOSABLE SCREENS
// =============================================================== //

@Composable
fun GradientBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepPurple,
                        Color(0xFF1A193B) // A slightly darker purple for the bottom
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun StoryApp(modifier: Modifier = Modifier, viewModel: StoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    GradientBox {
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
                path = uiState.chosenPath ?: "",
                onStorySelected = { story -> viewModel.startStory(story) }
            )
            Screen.STORY_SCREEN -> StoryScreen(
                uiState = uiState,
                onBack = { viewModel.goBackToStart() }
            )
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
        Text("Who Won The Bet?", style = MaterialTheme.typography.headlineLarge, color = LightPink)
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
        Text(text = "Congratulations, $winnerName!", style = MaterialTheme.typography.headlineLarge, color = LightPink, textAlign = TextAlign.Center)
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
fun StorySelectionScreen(winnerName: String, path: String, onStorySelected: (StoryScript) -> Unit) {
    val dominantGender = if (winnerName.equals("Josh", ignoreCase = true)) "Male" else "Female"
    val stories = StoryRepository.getAllStories().filter {
        it.type.equals(path, ignoreCase = true) && (it.dominantGender == null || it.dominantGender.equals(dominantGender, ignoreCase = true))
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                text = "Choose a $path",
                style = MaterialTheme.typography.headlineLarge,
                color = LightPink,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(stories) { story ->
            Card(
                modifier = Modifier.padding(vertical = 8.dp).clickable { onStorySelected(story) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val conceptText = story.concept.replace("{winner}", winnerName, ignoreCase = true)
                    Text(text = story.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = conceptText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun StoryScreen(uiState: StoryUiState, onBack: () -> Unit) {
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
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
                                        Text(text = "$speakerToDisplay: $textToDisplay", style = MaterialTheme.typography.bodyLarge)
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
                                            val commandText = command.text.replace("{winner}", uiState.winnerName, true).replace("{loser}", uiState.loserName, true)
                                            Text(
                                                text = commandText,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyLarge
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
