package com.redstoner.plots.model

import com.redstoner.plots.math.Vec2i

class Plot(val world: PlotWorld,
           val coord: Vec2i,
           data: PlotData? = null) {

    val id get() = "${coord.x}:${coord.z}"

    var data: PlotData? = data
        get() {
            if (field === null) {

            }

            return field
        }
        set(data) { field = data }

    val isLoaded: Boolean get() = data !== null

    override fun equals(other: Any?): Boolean = (this === other) || (other is Plot && world == other.world && coord == other.coord)

    override fun hashCode(): Int = world.hashCode() + 31 * coord.hashCode()

}