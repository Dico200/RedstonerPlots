package com.redstoner.plots.storage

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.model.Plot
import com.redstoner.plots.model.PlotWorld
import kotlin.coroutines.experimental.buildIterator

abstract class PlotRange : Sequence<Plot> {
    abstract val world: PlotWorld

    abstract fun <T> forEach(block: (Plot) -> T): Map<Plot, T>
}

class CoordinatePlotRange(override val world: PlotWorld,
                          min: Vec2i,
                          max: Vec2i) : PlotRange() {
    private val min: Vec2i = min.copy()
    private val max: Vec2i = max.copy()

    init {
        var tmp: Int
        if (min.x > max.x) {
            tmp = min.x
            min.x = max.x
            max.x = tmp
        }
        if (min.z > max.z) {
            tmp = min.z
            min.z = max.z
            max.z = tmp
        }
    }

    override fun iterator(): Iterator<Plot> = buildIterator {
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                yield(world.plotByID(x, z) ?: continue)
            }
        }
    }

    override fun <T> forEach(block: (Plot) -> T): Map<Plot, T> {
        val out: MutableMap<Plot, T> = HashMap()
        val world = this.world
        var plot: Plot
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                plot = world.plotByID(x, z) ?: continue
                out.put(plot, block(plot))
            }
        }
        return out
    }

}

class CollectionPlotRange(override val world: PlotWorld,
                          val coll: Sequence<Plot>) : PlotRange(), Sequence<Plot> by coll {

    override fun <T> forEach(block: (Plot) -> T): Map<Plot, T> {
        val out: MutableMap<Plot, T> = HashMap()
        coll.forEach {
            out.put(it, block(it))
        }
        return out
    }

}
