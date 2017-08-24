package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.math.floor
import kotlinx.coroutines.experimental.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import kotlin.coroutines.experimental.buildSequence

val worlds: Map<String, PlotWorld> get() = _worlds
private val _worlds: MutableMap<String, PlotWorld> = HashMap()

fun getWorld(name: String): PlotWorld? = _worlds.get(name)

fun getWorld(world: World): PlotWorld? = getWorld(world.name)

fun getPlotAt(block: Block): Plot? = getPlotAt(block.world, block.x, block.z)

fun getPlotAt(player: Player): Plot? = getPlotAt(player.location)

fun getPlotAt(location: Location): Plot? = getPlotAt(location.world, location.x.floor(), location.z.floor())

fun getPlotAt(world: World, x: Int, z: Int): Plot? = getPlotAt(world.name, x, z)

fun getPlotAt(world: String, x: Int, z: Int): Plot? {
    with (getWorld(world) ?: return null) {
        return generator.plotAt(x, z)
    }
}

fun loadWorlds(options: Options) {
    for ((worldName, worldOptions) in options.worlds.entries) {
        val world: PlotWorld
        try {
            world = PlotWorld(worldName, worldOptions, worldOptions.generator.getGenerator(worldName))
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

interface PlotProvider {

    fun plotAt(x: Int, z: Int): Plot?

    fun plotAt(vec: Vec2i): Plot? = plotAt(vec.x, vec.z)

    fun plotAt(loc: Location): Plot? = plotAt(loc.x.floor(), loc.z.floor())

    fun plotAt(entity: Entity): Plot? = plotAt(entity.location)

    fun plotAt(block: Block): Plot? = plotAt(block.x, block.z)
}

class PlotWorld(val name: String,
                val options: WorldOptions,
                val generator: PlotGenerator) : PlotProvider by generator {
    val world: World by lazy {
        val tmp = Bukkit.getWorld(name)
        if (tmp == null) {
            throw NullPointerException("World $name does not appear to be loaded")
        }
        tmp
    }

    val container: PlotContainer = DefaultPlotContainer(this)

    fun plotByID(x: Int, z: Int): Plot? {
        TODO("not implemented")
    }

    fun plotByID(id: Vec2i): Plot? = plotByID(id.x, id.z)

    fun enforceOptionsIfApplicable() {
        val world = world
        val options = options
        if (options.dayTime) {
            world.setGameRuleValue("doDaylightCycle", "false")
            world.setTime(6000)
        }

        if (options.noWeather) {
            world.setStorm(false)
            world.setThundering(false)
            world.weatherDuration = Integer.MAX_VALUE
        }

        world.setGameRuleValue("doTileDrops", "${options.doTileDrops}")
    }

}

abstract class PlotContainer {

    abstract fun ployByID(x: Int, z: Int): Plot?

    abstract fun nextEmptyPlot(): Plot?

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
                cur?.ployByID(x, z) ?: Plot(world, Vec2i(x, z))
            })
        })
    }

    override fun ployByID(x: Int, z: Int): Plot? {
        return plots[x][z]
    }

    override fun nextEmptyPlot(): Plot? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun allPlots(): Sequence<Plot> = buildSequence {
        for (array in plots) {
            yieldAll(array.iterator())
        }
    }

    fun loadAllData() {
        val channel = Main.instance.storage.readPlotData(allPlots(), 100).channel
        launch(Main.instance.storage.asyncDispatcher) {
            for ((plot, data) in channel) {
                if (data != null) {
                    plot.data = data
                }
            }
        }
    }

}