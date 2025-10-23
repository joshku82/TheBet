package com.joshbeth.thebet

object CommandRepository {

    fun getRandomCommand(category: String, fromLibrary: CommandLibrary, usedCommands: Set<StoryCommand>): StoryCommand? {
        val commandListSource: List<StoryCommand>? = when {
            category.contains(".") -> {
                val parts = category.split(".", limit = 2)
                val mainCategory = parts[0]
                val subCategory = parts[1]
                when (mainCategory) {
                    "toy_use" -> fromLibrary.toy_use?.get(subCategory)
                    "kinkActions" -> fromLibrary.kinkActions?.get(subCategory)
                    "aftercare" -> fromLibrary.aftercare?.get(subCategory)
                    else -> null
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
                    else -> null
                }
            }
        }

        val availableCommands = commandListSource?.filter { it !in usedCommands }

        return availableCommands?.randomOrNull()
    }
}
