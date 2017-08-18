package com.redstoner.plots.model

import com.redstoner.plots.math.Vec2i

class DefaultPlotContainer(private val world: PlotWorld) : PlotContainer() {
    private var plots: Array<Array<Plot>>

    init {
        plots = initArray(world.options.axisLimit, world)
    }

    fun resizeIfSizeChanged() {
        if (plots.size / 2 != world.options.axisLimit) {
            resize(world.options.axisLimit)
        }
    }

    fun resize(axisLimit: Int) {
        plots = initArray(axisLimit, world, this)
    }

    fun initArray(axisLimit: Int, world: PlotWorld, cur: DefaultPlotContainer? = null): Array<Array<Plot>> {
        val arraySize = 2 * axisLimit + 1
        return Array(arraySize, {
            val x = it - axisLimit
            Array(arraySize, {
                val z = it - axisLimit
                cur?.plotAt(x, z) ?: Plot(world, Vec2i(x, z))
            })
        })
    }

    override fun plotAt(x: Int, z: Int): Plot? {
        return plots[x][z]
    }

}