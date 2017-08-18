package com.redstoner.plots.model.generator

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class BlockTypeSerializer : StdSerializer<BlockType>(BlockType::class.java) {

    override fun serialize(value: BlockType?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeString(value?.toString())
    }

}