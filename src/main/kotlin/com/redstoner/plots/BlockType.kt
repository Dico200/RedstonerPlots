package com.redstoner.plots

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.bukkit.Material
import org.bukkit.block.Block

data class BlockType(val id: Short,
                     val data: Byte = 0) {

    init {
        Material.getMaterial(intId) ?: throw IllegalArgumentException()
    }

    val intId get() = id.toInt()

    val material get() = Material.getMaterial(intId) ?: throw InternalError()

    constructor(type: Material, data: Byte = 0) : this(type.id.toShort(), data)

    fun setBlock(block: Block, applyPhysics: Boolean = false): Boolean = block.setTypeIdAndData(intId, data, applyPhysics)

    fun toInt(): Int = intId.shl(16).or(data.toInt())

    override fun toString(): String {
        if (data == 0.toByte()) {
            return id.toString()
        }
        return id.toString() + data.toString()
    }

    companion object {
        val AIR = BlockType(Material.AIR)

        @Throws(IllegalArgumentException::class)
        fun fromString(str: String): BlockType {
            var colonIdx = str.indexOf(':')
            if (colonIdx == -1) {
                colonIdx = str.length
            }

            try {
                val id = str.substring(0, colonIdx).toShort()
                val data = if (colonIdx == str.length) {
                    0.toByte()
                } else {
                    str.substring(colonIdx, str.length).toByte()
                }

                return BlockType(id, data)
            } catch (ex: NumberFormatException) {
                throw IllegalArgumentException(ex)
            }
        }

    }

}

class BlockTypeDeserializer : StdDeserializer<BlockType>(BlockType::class.java) {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): BlockType {
        return BlockType.fromString(p!!.valueAsString)
    }

}


class BlockTypeSerializer : StdSerializer<BlockType>(BlockType::class.java) {

    override fun serialize(value: BlockType?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeString(value?.toString())
    }

}