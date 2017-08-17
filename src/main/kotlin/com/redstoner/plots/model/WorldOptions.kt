package com.redstoner.plots.model

import com.redstoner.plots.model.generator.DefaultGeneratorOptions
import com.redstoner.plots.model.generator.GeneratorOptions
import org.bukkit.GameMode
import org.bukkit.Material
import java.util.*

data class WorldOptions(var gameMode: GameMode = GameMode.CREATIVE,
                        var dayTime: Boolean = true,
                        var noWeather: Boolean = true,
                        var dropEntityItems: Boolean = true,
                        var doTileDrops: Boolean = false,
                        var disableExplosions: Boolean = true,
                        var blockPortalCreation: Boolean = true,
                        var blockMobSpawning: Boolean = true,
                        var blockedItems: Set<Material> = EnumSet.of(Material.AIR),
                        var generator: GeneratorOptions = DefaultGeneratorOptions()) {
}