package com.redstoner.plots.model

import org.bukkit.Bukkit
import org.bukkit.World
import java.util.*

class SerializableWorld(val name: String? = null,
                        val uid: UUID? = null) {

    init {
        uid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val world: World? by lazy { lookupWorld() }

    private fun lookupWorld(): World? {
        return Bukkit.getWorld(uid ?: return Bukkit.getWorld(name ?: return null))
    }

    val plotWorld: PlotWorld? by lazy { lookupPlotWorld() }

    private fun lookupPlotWorld(): PlotWorld? {
        TODO("not implemented")
    }

}