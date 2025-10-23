package com.joshbeth.thebet

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object StoryRepository {

    private val json = Json { ignoreUnknownKeys = true } // Lenient parser

    @Volatile
    private var stories: List<StoryScript> = emptyList()

    /**
     * Loads and parses stories from the assets/stories.json file asynchronously.
     * This should be called once, ideally from a coroutine scope.
     */
    suspend fun loadStories(context: Context) {
        if (stories.isNotEmpty()) return // Avoid reloading

        withContext(Dispatchers.IO) {
            val jsonString = context.assets.open("stories.json").bufferedReader().use { it.readText() }
            stories = json.decodeFromString(jsonString)
        }
    }

    /**
     * Returns the list of all loaded stories.
     */
    fun getAllStories(): List<StoryScript> {
        return stories
    }

    /**
     * Finds a specific story by its ID.
     */
    fun getStoryById(id: String): StoryScript? {
        return stories.find { it.id == id }
    }
}
