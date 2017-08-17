package com.redstoner.plots.math

data class Vec2i(var x: Int, var z: Int) {

    fun toVec3i(y: Int) = Vec3i(x, y, z)

}