package com.redstoner.plots

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.redstoner.plots.util.Jackson
import com.redstoner.plots.util.substringFrom
import com.redstoner.plots.util.toIntOr
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Biome
import java.io.Reader
import java.io.Writer
import java.util.*

class Options {
    var worlds: Map<String, WorldOptions> = HashMap()
        private set

    fun addWorld(name: String, options: WorldOptions) = (worlds as MutableMap).put(name, options)

    fun addDefaultWorld() = addWorld("plotworld", WorldOptions())

    fun writeTo(writer: Writer) = Jackson.yamlObjectMapper.writeValue(writer, this)

    fun mergeFrom(reader: Reader) = Jackson.yamlObjectMapper.readerForUpdating(this).readValue<Options>(reader)

    override fun toString(): String = Jackson.yamlObjectMapper.writeValueAsString(this)

    companion object {
        fun loadFrom(reader: Reader): Options = Jackson.yamlObjectMapper.readValue(reader, Options::class.java)
    }

}

data class WorldOptions(var gameMode: GameMode = GameMode.CREATIVE,
                        var dayTime: Boolean = true,
                        var noWeather: Boolean = true,
                        var dropEntityItems: Boolean = true,
                        var doTileDrops: Boolean = false,
                        var disableExplosions: Boolean = true,
                        var blockPortalCreation: Boolean = true,
                        var blockMobSpawning: Boolean = true,
                        var blockedItems: Set<Material> = EnumSet.of(Material.AIR),
                        var axisLimit: Int = 10,
                        var generator: GeneratorOptions = DefaultGeneratorOptions()) {
}

abstract class GeneratorOptions {

    abstract fun generatorFactory(): GeneratorFactory

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


class GeneratorOptionsDeserializer : JsonDeserializer<GeneratorOptions>() {

    override fun deserialize(parser: JsonParser?, ctx: DeserializationContext?): GeneratorOptions? {
        val node = parser!!.readValueAsTree<JsonNode>()
        val name = node.get("name").asText()
        val optionsNode = node.get("options")
        val factory = GeneratorFactory.getFactory(name) ?: throw IllegalStateException("Unknown generator: $name")

        return parser.codec.treeToValue(optionsNode, factory.optionsClass.java)
    }

}

class GeneratorOptionsSerializer(private val defaultSerializer: JsonSerializer<GeneratorOptions>) : JsonSerializer<GeneratorOptions>() {

    override fun serialize(input: GeneratorOptions?, generator: JsonGenerator?, provider: SerializerProvider?) {
        with(generator!!) {
            writeStartObject()
            writeFieldName("name")
            writeString(input!!.generatorFactory().name)
            writeFieldName("options")
            defaultSerializer.serialize(input, generator, provider)
            writeEndObject()
        }
    }

}

data class DataStorageOptions(val address: String,
                              val database: String,
                              val username: String,
                              val password: String,
                              val poolSize: Int) {

    fun splitAddressAndPort(defaultPort: Int = 3306): Pair<String, Int>? {
        val idx = address.indexOf(":")
        if (idx == -1) {
            return Pair(address, 3306)
        }
        val addressName = address.substring(0, idx)
        if (addressName.isBlank()) {
            Main.instance.logger.severe("(Invalidly) blank address in data storage options")
            return null
        }

        val port = address.substringFrom(idx).toIntOr {
            Main.instance.logger.severe("Invalid port number in data storage options: $it, using 3306 as default")
            return null
        }
        return Pair(addressName, port)
    }

}