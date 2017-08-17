package com.redstoner.plots.model.generator

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class BlockTypeDeserializer : StdDeserializer<BlockType>(BlockType::class.java) {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): BlockType {
        return BlockType.fromString(p?.valueAsString ?: throw NullPointerException())
    }

}