package com.redstoner.plots.model.generator

import org.bukkit.Material
import org.bukkit.block.Biome

data class DefaultGeneratorOptions(val defaultBiome: Biome = Biome.JUNGLE,
                                   val wallType: BlockType = BlockType(Material.STEP),
                                   val floorType: BlockType = BlockType(Material.QUARTZ_BLOCK),
                                   val fillType: BlockType = BlockType(Material.QUARTZ_BLOCK),
                                   val pathMainType: BlockType = BlockType(Material.SANDSTONE),
                                   val pathAltType: BlockType = BlockType(Material.REDSTONE_BLOCK),
                                   val plotSize: Int = 101,
                                   val pathSize: Int = 9,
                                   val floorHeight: Int = 64,
                                   val offsetX: Int = 0,
                                   val offsetZ: Int = 0) : GeneratorOptions() {

    @Transient
    val sectionSize = plotSize + pathSize
    @Transient
    val pathOffset = (if (pathSize % 2 == 0) pathSize + 2 else pathSize + 1) / 2

    @Transient
    val makePathMain = pathSize > 2
    @Transient
    val makePathAlt = pathSize > 4

    override fun generatorFactory(): GeneratorFactory = DefaultPlotGenerator

}