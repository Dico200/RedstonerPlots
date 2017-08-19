package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.math.floor
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import java.util.*

object WorldManager {
    val worlds: Map<String, PlotWorld> get() = _worlds
    private val _worlds: MutableMap<String, PlotWorld> = HashMap()

    fun getWorld(name: String): PlotWorld? = _worlds.get(name)

    fun getWorld(world: World): PlotWorld? = getWorld(world.name)

    fun loadConfigOptions(options: Options) {
        for ((worldName, worldOptions) in options.worlds.entries) {
            val world: PlotWorld
            try {
                world = PlotWorld(worldName, worldOptions)
            } catch (ex: Exception) {
                ex.printStackTrace()
                continue
            }

            _worlds.put(worldName, world)

            if (Bukkit.getWorld(worldName) == null) {
                val bworld = WorldCreator(worldName).generator(world.generator).createWorld()
                val spawn = world.generator.getFixedSpawnLocation(bworld, null)
                bworld.setSpawnLocation(spawn.x.floor(), spawn.y.floor(), spawn.z.floor())
            }
        }

    }

}

class PlotWorld(val name: String,
                val options: WorldOptions) {
    val generator = options.generator.generatorFactory().newPlotGenerator(this)
    val world: World by lazy {
        val tmp = Bukkit.getWorld(name)
        if (tmp == null) {
            throw NullPointerException("World $name does not appear to be loaded")
        }
        tmp
    }

    val container = DefaultPlotContainer(this)

    fun plotByID(x: Int, z: Int): Plot? {
        TODO("not implemented")
    }

    fun plotByID(id: Vec2i): Plot? = plotByID(id.x, id.z)
}



abstract class PlotContainer {

    abstract fun plotAt(x: Int, z: Int): Plot?

}

class DefaultPlotContainer(private val world: PlotWorld) : PlotContainer() {
    private var plots: Array<Array<Plot>>

    init {
        plots = initArray(world.options.axisLimit, world)
    }

    fun resizeIfSizeChanged() {
        if (plots.size / 2 != world.options.axisLimit) {
            resize(world.options.axisLimit)
        }
    }

    fun resize(axisLimit: Int) {
        plots = initArray(axisLimit, world, this)
    }

    fun initArray(axisLimit: Int, world: PlotWorld, cur: DefaultPlotContainer? = null): Array<Array<Plot>> {
        val arraySize = 2 * axisLimit + 1
        return Array(arraySize, {
            val x = it - axisLimit
            Array(arraySize, {
                val z = it - axisLimit
                cur?.plotAt(x, z) ?: Plot(world, Vec2i(x, z))
            })
        })
    }

    override fun plotAt(x: Int, z: Int): Plot? {
        return plots[x][z]
    }

}