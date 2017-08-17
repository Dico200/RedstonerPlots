package com.redstoner.plots.model.generator

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.redstoner.plots.util.Jackson
import org.bukkit.Material

data class BlockType(val id: Short,
                     val data: Byte = 0) {

    constructor(type: Material, data: Byte = 0) : this(type.id.toShort(), data)

    companion object {

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

        init {
            Jackson.yamlObjectMapper.registerKotlinModule()
        }

    }

    fun toInt(): Int {
        return id.toInt().shl(16).or(data.toInt())
    }

    override fun toString(): String {
        if (data == 0.toByte()) {
            return id.toString()
        }
        return id.toString() + data.toString()
    }

}