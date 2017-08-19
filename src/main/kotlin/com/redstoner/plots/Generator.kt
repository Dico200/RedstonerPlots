package com.redstoner.plots

import com.redstoner.plots.math.*
import org.bukkit.*
import org.bukkit.block.*
import org.bukkit.entity.Entity
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import java.util.*
import kotlin.coroutines.experimental.buildIterator
import kotlin.reflect.KClass

abstract class PlotGenerator(val world: PlotWorld) : ChunkGenerator() {

    abstract val factory: GeneratorFactory

    abstract override fun generateChunkData(world: World?, random: Random?, chunkX: Int, chunkZ: Int, biome: BiomeGrid?): ChunkData

    abstract fun populate(world: World?, random: Random?, chunk: Chunk?)

    abstract override fun getFixedSpawnLocation(world: World?, random: Random?): Location

    override fun getDefaultPopulators(world: World?): MutableList<BlockPopulator> {
        return Collections.singletonList(object : BlockPopulator() {
            override fun populate(world: World?, random: Random?, chunk: Chunk?) {
                this@PlotGenerator.populate(world, random, chunk)
            }
        })
    }

    abstract fun plotAt(x: Int, z: Int): Plot?

    fun plotAt(vec: Vec2i): Plot? = plotAt(vec.x, vec.z)

    fun plotAt(loc: Location): Plot? = plotAt(loc.x.floor(), loc.z.floor())

    fun plotAt(block: Block): Plot? = plotAt(block.x, block.z)

    abstract fun updateOwner(plot: Plot)

    abstract fun getBottomCoord(plot: Plot): Vec2i

    abstract fun getHomeLocation(plot: Plot): Location

    abstract fun setBiome(plot: Plot, biome: Biome)

    abstract fun getEntities(plot: Plot): Collection<Entity>

    abstract fun getBlocks(plot: Plot, yRange: IntRange = 0..255): Iterator<Block>

}


interface GeneratorFactory {

    val name: String

    val optionsClass: KClass<out GeneratorOptions>

    fun newPlotGenerator(world: PlotWorld): PlotGenerator

}

object GeneratorFactories {
    private val map: MutableMap<String, GeneratorFactory> = HashMap()

    fun registerFactory(generator: GeneratorFactory): Boolean = map.putIfAbsent(generator.name, generator) == null

    fun getFactory(name: String): GeneratorFactory? = map.get(name)

}

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


class DefaultPlotGenerator(world: PlotWorld) : PlotGenerator(world) {
    private val o = world.options.generator as DefaultGeneratorOptions

    override val factory = Factory

    companion object Factory : GeneratorFactory {
        override val name get() = "default"
        override val optionsClass get() = DefaultGeneratorOptions::class
        override fun newPlotGenerator(world: PlotWorld): PlotGenerator = DefaultPlotGenerator(world)
    }

    private inline fun <T> generate(chunkX: Int, chunkZ: Int, floor: T, wall: T, pathMain: T, pathAlt: T, fill: T, setter: (Int, Int, Int, T) -> Unit) {
        val sectionSize = o.sectionSize
        val pathOffset = o.pathOffset
        val floorHeight = o.floorHeight
        val plotSize = o.plotSize
        val makePathMain = o.makePathMain
        val makePathAlt = o.makePathAlt

        // plot bottom x and z
        // umod is unsigned %: the result is always >= 0
        val pbx = ((chunkX shl 4) - o.offsetX) umod sectionSize
        val pbz = ((chunkZ shl 4) - o.offsetZ) umod sectionSize

        var curHeight: Int
        var x: Int
        var z: Int
        for (cx in 0..15) {
            for (cz in 0..15) {
                x = (pbx + cx) % sectionSize - pathOffset
                z = (pbz + cz) % sectionSize - pathOffset
                curHeight = floorHeight

                val type = when {
                    (0 <= x && x < plotSize && 0 <= z && z <= plotSize) -> floor
                    (-1 <= x && x <= plotSize && -1 <= z && z <= plotSize) -> {
                        curHeight++
                        wall
                    }
                    (makePathAlt && -2 <= x && x <= plotSize + 1 && -2 <= z && z <= plotSize + 1) -> pathAlt
                    (makePathMain) -> pathMain
                    else -> {
                        curHeight++
                        wall
                    }
                }

                for (y in 0 until curHeight) {
                    setter(x, y, z, fill)
                }
                setter(x, curHeight, z, type)
            }
        }
    }

    override fun generateChunkData(world: World?, random: Random?, chunkX: Int, chunkZ: Int, biome: BiomeGrid?): ChunkData {
        val out = Bukkit.createChunkData(world)
        generate(chunkX, chunkZ, o.floorType.id, o.wallType.id, o.pathMainType.id, o.pathAltType.id, o.fillType.id) { x, y, z, type ->
            out.setBlock(x, y, z, type.toInt(), 0.toByte())
        }
        return out
    }

    override fun populate(world: World?, random: Random?, chunk: Chunk?) {
        generate(chunk!!.x, chunk.z, o.floorType.data, o.wallType.data, o.pathMainType.data, o.pathAltType.data, o.fillType.data) { x, y, z, type ->
            if (type == 0.toByte()) chunk.getBlock(x, y, z).setData(type, false)
        }
    }

    override fun getFixedSpawnLocation(world: World?, random: Random?): Location {
        val fix = if (o.plotSize.even) 0.5 else 0.0
        return Location(world, o.offsetX + fix, o.floorHeight + 1.0, o.offsetZ + fix)
    }

    override fun plotAt(x: Int, z: Int): Plot? {
        val sectionSize = o.sectionSize
        val plotSize = o.plotSize
        val absX = x - o.offsetX - o.pathOffset
        val absZ = z - o.offsetZ - o.pathOffset
        val modX = absX umod sectionSize
        val modZ = absZ umod sectionSize
        if (0 <= modX && modX < plotSize && 0 <= modZ && modZ < plotSize) {
            return world.plotByID((absX - modX) / sectionSize, (absZ - modZ) / sectionSize)
        }
        return null
    }

    override fun getBottomCoord(plot: Plot): Vec2i = Vec2i(o.sectionSize * plot.coord.x + o.pathOffset + o.offsetX,
            o.sectionSize * plot.coord.z + o.pathOffset + o.offsetZ)

    override fun getHomeLocation(plot: Plot): Location {
        val bottom = getBottomCoord(plot)
        return Location(world.world, bottom.x.toDouble(), o.floorHeight + 1.0, bottom.z + (o.plotSize - 1) / 2.0, -90F, 0F)
    }

    override fun updateOwner(plot: Plot) {
        val world = this.world.world
        val b = getBottomCoord(plot)

        val wallBlock = world.getBlockAt(b.x - 1, o.floorHeight + 1, b.z - 1)
        val signBlock = world.getBlockAt(b.x - 2, o.floorHeight + 1, b.z - 1)
        val skullBlock = world.getBlockAt(b.x - 1, o.floorHeight + 2, b.z - 1)

        val owner = plot.data?.owner
        if (owner == null) {
            o.wallType.setBlock(wallBlock)
            BlockType.AIR.setBlock(signBlock)
            BlockType.AIR.setBlock(skullBlock)
        } else {
            val wallBlockType = o.wallType.copy(when (o.wallType.material) {
                Material.CARPET -> Material.WOOL
                Material.STEP -> Material.DOUBLE_STEP
                Material.WOOD_STEP -> Material.WOOD_DOUBLE_STEP
                else -> o.wallType.material
            }.id.toShort())
            wallBlockType.setBlock(wallBlock)

            BlockType(Material.WALL_SIGN, 4.toByte()).setBlock(signBlock)
            val sign = signBlock.state as Sign
            sign.setLine(0, plot.id)
            sign.setLine(2, owner.playerName)
            sign.update()

            BlockType(Material.SKULL, 1.toByte()).setBlock(skullBlock)
            val skull = skullBlock.state as Skull
            if (owner.uuid != null) {
                skull.owningPlayer = owner.offlinePlayer
            } else {
                skull.owner = owner.name
            }
            skull.rotation = BlockFace.WEST
            skull.update()
        }
    }

    override fun setBiome(plot: Plot, biome: Biome) {
        val world = this.world.world
        val b = getBottomCoord(plot)
        val plotSize = o.plotSize
        for (x in b.x until b.x + plotSize) {
            for (z in b.z until b.z + plotSize) {
                world.setBiome(x, z, biome)
            }
        }
    }

    override fun getEntities(plot: Plot): Collection<Entity> {
        val world = this.world.world
        val b = getBottomCoord(plot)
        val plotSize = o.plotSize
        val center = Location(world, (b.x + plotSize) / 2.0, 128.0, (b.z + plotSize) / 2.0)
        return world.getNearbyEntities(center, plotSize / 2.0 + 0.2, 128.0, plotSize / 2.0 + 0.2)
    }

    override fun getBlocks(plot: Plot, yRange: IntRange): Iterator<Block> = buildIterator {
        val range = yRange.clamp(0, 255)
        val world = this@DefaultPlotGenerator.world.world
        val b = getBottomCoord(plot)
        val plotSize = o.plotSize
        for (x in b.x until b.x + plotSize) {
            for (z in b.z until b.z + plotSize) {
                for (y in range) {
                    yield(world.getBlockAt(x, y, z))
                }
            }
        }
    }

}