package com.joshbeth.thebet

object CommandRepository {

    fun getRandomCommand(category: String, fromLibrary: CommandLibrary, usedCommands: Set<StoryCommand>): StoryCommand? {
        val commandListSource: List<StoryCommand>? = getCommandsForCategory(category, fromLibrary)

        val availableCommands = commandListSource?.filter { it !in usedCommands }

        return availableCommands?.randomOrNull()
    }

    fun getCommandsForCategory(category: String, fromLibrary: CommandLibrary): List<StoryCommand> {
        return when {
            category.contains(".") -> {
                val parts = category.split(".", limit = 2)
                val mainCategory = parts[0]
                val subCategory = parts[1]
                when (mainCategory) {
                    "toy_use" -> fromLibrary.toy_use?.get(subCategory)
                    "kinkActions" -> fromLibrary.kinkActions?.get(subCategory)
                    "aftercare" -> fromLibrary.aftercare?.get(subCategory)
                    else -> emptyList()
                }
            }
            else -> {
                when (category) {
                    "instruction" -> fromLibrary.instruction
                    "instructiona" -> fromLibrary.instructiona
                    "humiliation" -> fromLibrary.humiliation
                    "praise" -> fromLibrary.praise
                    "actionsDomOnSubHandsOnBody" -> fromLibrary.actionsDomOnSubHandsOnBody
                    "actionsDomOnSubHandsOnPussy" -> fromLibrary.actionsDomOnSubHandsOnPussy
                    "actionsDomOnSubHandsOnCock" -> fromLibrary.actionsDomOnSubHandsOnCock
                    "actionsSubOnDomMouthOnPussy" -> fromLibrary.actionsSubOnDomMouthOnPussy
                    "actionsSubOnDomMouthOnCock" -> fromLibrary.actionsSubOnDomMouthOnCock
                    "subToDomWorship" -> fromLibrary.subToDomWorship
                    "position" -> fromLibrary.position
                    "intensity" -> fromLibrary.intensity
                    "setup_humiliation" -> fromLibrary.setup_humiliation
                    "instruction_climax" -> fromLibrary.instruction_climax
                    "positioning" -> fromLibrary.positioning
                    "climax_instruction" -> fromLibrary.climax_instruction
                    "instruction_force_position" -> fromLibrary.instruction_force_position
                    else -> emptyList()
                }
            }
        } ?: emptyList()
    }
}
