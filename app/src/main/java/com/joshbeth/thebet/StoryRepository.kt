package com.joshbeth.thebet

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

object StoryRepository {

    private val json = Json { ignoreUnknownKeys = true } // Lenient parser

    private var stories: List<StoryScript> = emptyList()

    /**
     * Loads and parses stories from the assets/stories.json file.
     * This should be called once, ideally from the Application class or a ViewModel init block.
     */
    fun loadStories(context: Context) {
        if (stories.isNotEmpty()) return // Avoid reloading

        val jsonString = context.assets.open("stories.json").bufferedReader().use { it.readText() }
        stories = json.decodeFromString(jsonString)
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
