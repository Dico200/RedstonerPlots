package com.redstoner.plots.math

import org.bukkit.Location

data class Vec3d(val x: Double, val y: Double, val z: Double) {

    companion object {

        fun location(location: Location) = Vec3d(location.x, location.y, location.z)

    }

    fun toVec3i() = Vec3i(NumberExtensions.floor(x), NumberExtensions.floor(y), NumberExtensions.floor(z))

}