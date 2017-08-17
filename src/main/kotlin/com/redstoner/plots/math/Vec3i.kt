package com.redstoner.plots.math

import org.bukkit.block.Block

data class Vec3i(var x: Int, var y: Int, var z: Int) {

    companion object {
        fun block(block: Block) = Vec3i(block.x, block.y, block.z)
    }

    fun toVec2i() = Vec2i(x, z)

}