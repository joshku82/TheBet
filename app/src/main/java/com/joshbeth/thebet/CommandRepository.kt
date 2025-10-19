package com.joshbeth.thebet

/**
 * Repository responsible for retrieving commands from a given CommandLibrary.
 * This is designed to work with story-specific libraries.
 */
object CommandRepository {

    /**
     * Draws a random command from the provided library based on the category string.
     *
     * @param category The category of the command to draw (e.g., "humiliation", "kinkActions.for_her").
     * @param fromLibrary The specific CommandLibrary instance to draw from.
     * @return A random StoryCommand or null if the category is not found or is empty.
     */
    fun getRandomCommand(category: String, fromLibrary: CommandLibrary): StoryCommand? {
        // Handle nested categories like "kinkActions.for_her" or "aftercare.verbal"
        if (category.contains(".")) {
            val parts = category.split(".", limit = 2)
            val mainCategory = parts[0]
            val subCategory = parts[1]

            val commandMap = when (mainCategory) {
                "kinkActions" -> fromLibrary.kinkActions
                "aftercare" -> fromLibrary.aftercare
                "toy_use" -> fromLibrary.toy_use
                else -> null
            }
            return commandMap?.get(subCategory)?.randomOrNull()
        }

        // Handle top-level categories
        return when (category) {
            "instruction" -> fromLibrary.instruction?.randomOrNull()
            "humiliation" -> fromLibrary.humiliation?.randomOrNull()
            "praise" -> fromLibrary.praise?.randomOrNull()
            "actionsDomOnSubHandsOnBody" -> fromLibrary.actionsDomOnSubHandsOnBody?.randomOrNull()
            "actionsDomOnSubHandsOnPussy" -> fromLibrary.actionsDomOnSubHandsOnPussy?.randomOrNull()
            "actionsDomOnSubHandsOnCock" -> fromLibrary.actionsDomOnSubHandsOnCock?.randomOrNull()
            "actionsSubOnDomMouthOnPussy" -> fromLibrary.actionsSubOnDomMouthOnPussy?.randomOrNull()
            "actionsSubOnDomMouthOnCock" -> fromLibrary.actionsSubOnDomMouthOnCock?.randomOrNull()
            "subToDomWorship" -> fromLibrary.subToDomWorship?.randomOrNull()
            else -> null
        }
    }
}
