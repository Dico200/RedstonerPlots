package com.redstoner.plots.model.generator

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class GeneratorOptionsSerializer(private val defaultSerializer: JsonSerializer<GeneratorOptions>) : JsonSerializer<GeneratorOptions>() {

    override fun serialize(input: GeneratorOptions?, generator: JsonGenerator?, provider: SerializerProvider?) {
        input ?: throw NullPointerException()
        generator ?: throw NullPointerException()
        with(generator) {
            writeStartObject()
            writeFieldName("name")
            writeString(input.generatorFactory().name)
            writeFieldName("options")
            defaultSerializer.serialize(input, generator, provider)
            writeEndObject()
        }
    }

}