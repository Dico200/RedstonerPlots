package com.redstoner.plots

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.redstoner.plots.util.Jackson
import org.bukkit.GameMode
import org.bukkit.Material
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


class GeneratorOptionsDeserializer : JsonDeserializer<GeneratorOptions>() {

    override fun deserialize(parser: JsonParser?, ctx: DeserializationContext?): GeneratorOptions? {
        val node = parser!!.readValueAsTree<JsonNode>()
        val name = node.get("name").asText()
        val optionsNode = node.get("options")
        val factory = GeneratorFactories.getFactory(name) ?: throw IllegalStateException("Unknown generator: $name")

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

data class DataStorageOptions(var address: String,
                              var database: String,
                              var username: String,
                              var password: String,
                              var poolSize: Int) {
}