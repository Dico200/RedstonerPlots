package com.redstoner.plots.model

import com.redstoner.plots.math.Vec2i
import java.util.concurrent.atomic.AtomicInteger

class DefaultPlotContainer(private var axisLimit: Int, private val world: PlotWorld) : PlotContainer() {
    private var plots: Array<Array<Plot>>

    companion object {
        var nextId: AtomicInteger = AtomicInteger(0)

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
    }

    init {
        plots = initArray(axisLimit, world)
    }

    fun resize(axisLimit: Int) {
        this.axisLimit = axisLimit
        plots = initArray(axisLimit, world, this)
    }

    override fun plotAt(x: Int, z: Int): Plot? {
        return plots[x][z]
    }

}