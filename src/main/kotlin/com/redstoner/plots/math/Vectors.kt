package com.redstoner.plots.math

import org.bukkit.Location
import org.bukkit.block.Block

data class Vec2i(var x: Int, var z: Int) {

    fun toVec3i(y: Int) = Vec3i(x, y, z)

}

data class Vec3i(var x: Int, var y: Int, var z: Int) {

    companion object {
        fun block(block: Block) = Vec3i(block.x, block.y, block.z)
    }

    fun toVec2i() = Vec2i(x, z)

}

data class Vec3d(val x: Double, val y: Double, val z: Double) {

    companion object {

        fun location(location: Location) = Vec3d(location.x, location.y, location.z)

    }

    fun toVec3i() = Vec3i(x.floor(), y.floor(), z.floor())

}