package com.redstoner.plots.model.generator

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.model.Plot
import com.redstoner.plots.model.PlotWorld
import org.bukkit.Chunk
import org.bukkit.World
import java.util.*
import kotlin.reflect.KClass

class DefaultPlotGenerator(world: PlotWorld, val options: GeneratorOptions) : PlotGenerator(world) {

    override fun factory(): GeneratorFactory = Factory

    companion object Factory : GeneratorFactory {
        override val name: String
            get() = "default"
        override val optionsClass: KClass<out GeneratorOptions>
            get() = DefaultGeneratorOptions::class

        override fun newPlotGenerator(world: PlotWorld, options: GeneratorOptions): PlotGenerator = DefaultPlotGenerator(world, options)
    }

    override fun generateChunkData(world: World?, random: Random?, x: Int, z: Int, biome: BiomeGrid?): ChunkData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun populate(world: World?, random: Random?, chunk: Chunk?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateOwner(plot: Plot) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun plotAt(x: Int, z: Int): Plot? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHomeLocation(plot: Plot): Vec2i {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newPlot(id: Int): Plot {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}