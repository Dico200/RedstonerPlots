package com.redstoner.plots.model.generator

import com.redstoner.plots.math.NumberConversions
import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.model.Plot
import com.redstoner.plots.model.PlotWorld
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import java.util.*

abstract class PlotGenerator(val world: PlotWorld) : ChunkGenerator() {

    abstract fun factory(): GeneratorFactory

    abstract override fun generateChunkData(world: World?, random: Random?, x: Int, z: Int, biome: BiomeGrid?): ChunkData

    abstract fun populate(world: World?, random: Random?, chunk: Chunk?): Unit

    override fun getDefaultPopulators(world: World?): MutableList<BlockPopulator> {
        return Collections.singletonList(object : BlockPopulator() {
            override fun populate(world: World?, random: Random?, chunk: Chunk?) {
                this@PlotGenerator.populate(world, random, chunk)
            }
        })
    }

    abstract fun updateOwner(plot: Plot)

    abstract fun plotAt(x: Int, z: Int): Plot?

    fun plotAt(vec: Vec2i): Plot? = plotAt(vec.x, vec.z)

    fun plotAt(loc: Location): Plot? = plotAt(NumberConversions.floor(loc.x), NumberConversions.floor(loc.z))

    fun plotAt(block: Block): Plot? = plotAt(block.x, block.z)

    abstract fun getHomeLocation(plot: Plot): Vec2i

    abstract fun newPlot(id: Int): Plot

}