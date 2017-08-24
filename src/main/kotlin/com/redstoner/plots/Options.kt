package com.redstoner.plots

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.redstoner.plots.storage.Storage
import com.redstoner.plots.storage.StorageFactory
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
    var storage: StorageOptions = StorageOptions("mysql", StorageFactory.getFactory("mysql")!!, DataConnectionOptions())

    fun addWorld(name: String, options: WorldOptions) = (worlds as MutableMap).put(name, options)

    fun addDefaultWorld() = addWorld("plotworld", WorldOptions())

    fun writeTo(writer: Writer) = Jackson.yamlObjectMapper.writeValue(writer, this)

    fun mergeFrom(reader: Reader) = Jackson.yamlObjectMapper.readerForUpdating(this).readValue<Options>(reader)

    override fun toString(): String = Jackson.yamlObjectMapper.writeValueAsString(this)

}

data class WorldOptions(var gameMode: GameMode? = GameMode.CREATIVE,
                        var dayTime: Boolean = true,
                        var noWeather: Boolean = true,
                        var dropEntityItems: Boolean = true,
                        var doTileDrops: Boolean = false,
                        var disableExplosions: Boolean = true,
                        var blockPortalCreation: Boolean = true,
                        var blockMobSpawning: Boolean = true,
                        var blockedItems: Set<Material> = EnumSet.of(Material.FLINT_AND_STEEL, Material.SNOW_BALL),
                        var axisLimit: Int = 10,
                        var generator: GeneratorOptions = DefaultGeneratorOptions()) {

}

abstract class GeneratorOptions {

    abstract fun generatorFactory(): GeneratorFactory

    fun getGenerator(worldName: String) = generatorFactory().newPlotGenerator(worldName, this)

}

data class DefaultGeneratorOptions(var defaultBiome: Biome = Biome.JUNGLE,
                                   var wallType: BlockType = BlockType(Material.STEP),
                                   var floorType: BlockType = BlockType(Material.QUARTZ_BLOCK),
                                   var fillType: BlockType = BlockType(Material.QUARTZ_BLOCK),
                                   var pathMainType: BlockType = BlockType(Material.SANDSTONE),
                                   var pathAltType: BlockType = BlockType(Material.REDSTONE_BLOCK),
                                   var plotSize: Int = 101,
                                   var pathSize: Int = 9,
                                   var floorHeight: Int = 64,
                                   var offsetX: Int = 0,
                                   var offsetZ: Int = 0) : GeneratorOptions() {

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
            writeStringField("name", input!!.generatorFactory().name)
            writeFieldName("options")
            defaultSerializer.serialize(input, generator, provider)
            writeEndObject()
        }
    }

}

class StorageOptions(val method: String,
                     val storageFactory: StorageFactory,
                     val options: Any) {

    fun newStorageInstance(): Storage = storageFactory.newStorageInstance(method, options)

}

class StorageOptionsDeserializer : JsonDeserializer<StorageOptions>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): StorageOptions {
        val node = p!!.readValueAsTree<JsonNode>()
        val method = node.get("method").asText()
        val optionsNode = node.get("options")
        val factory = StorageFactory.getFactory(method) ?: throw IllegalStateException("Unknown storage method: $method")
        val options = p.codec.treeToValue(optionsNode, factory.optionsClass.java)
        return StorageOptions(method, factory, options)
    }

}

class StorageOptionsSerializer : StdSerializer<StorageOptions>(StorageOptions::class.java) {

    override fun serialize(value: StorageOptions?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        with(gen!!) {
            writeStartObject()
            writeStringField("method", value!!.method)
            writeFieldName("options")
            writeObject(value.options)
            writeEndObject()
        }
    }

}

data class DataConnectionOptions(val address: String = "localhost",
                                 val database: String = "redstonerplots",
                                 val username: String = "root",
                                 val password: String = "",
                                 val poolSize: Int = 4) {

    fun splitAddressAndPort(defaultPort: Int = 3306): Pair<String, Int>? {
        val idx = address.indexOf(":")
        if (idx == -1) {
            return Pair(address, defaultPort)
        }
        val addressName = address.substring(0, idx)
        if (addressName.isBlank()) {
            Main.instance.logger.severe("(Invalidly) blank address in data storage options")
            return null
        }

        val port = address.substringFrom(idx).toIntOr {
            Main.instance.logger.severe("Invalid port number in data storage options: $it, using $defaultPort as default")
            return null
        }
        return Pair(addressName, port)
    }

}

data class DataFileOptions(val location: String = "/flatfile-storage/")


