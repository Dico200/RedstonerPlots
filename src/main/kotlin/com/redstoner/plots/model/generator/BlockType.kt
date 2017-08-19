package com.redstoner.plots.model.generator

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