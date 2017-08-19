package com.redstoner.plots.storage

import com.redstoner.plots.PlotWorld
import com.redstoner.plots.math.Vec2i
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

/**
 * Used by storage backing options to encompass the location of a plot
 */
class SerializablePlot(val world: SerializableWorld,
                       val coord: Vec2i) {
}