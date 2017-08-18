package com.redstoner.plots.model.generator

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode


class GeneratorOptionsDeserializer : JsonDeserializer<GeneratorOptions>() {

    override fun deserialize(parser: JsonParser?, ctx: DeserializationContext?): GeneratorOptions? {
        val node = parser!!.readValueAsTree<JsonNode>()
        val name = node.get("name").asText()
        val optionsNode = node.get("options")
        val factory = GeneratorFactories.getFactory(name) ?: throw IllegalStateException("Unknown generator: $name")

        return parser.codec.treeToValue(optionsNode, factory.optionsClass.java)
    }

}