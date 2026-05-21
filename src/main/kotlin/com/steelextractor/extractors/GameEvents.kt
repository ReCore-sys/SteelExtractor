package com.steelextractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.steelextractor.SteelExtractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer

class GameEvents : SteelExtractor.Extractor {
    override fun fileName(): String {
        return "steel-registry/build_assets/game_events.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val obj = JsonObject()

        BuiltInRegistries.GAME_EVENT.asHolderIdMap().forEach { holder ->
            obj.addProperty(holder.registeredName, holder.value().notificationRadius)
        }

        return obj
    }
}