package com.redstoner.plots.model

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.model.generator.PlotGenerator
import org.bukkit.Bukkit
import org.bukkit.World

class PlotWorld(val name: String,
                val options: WorldOptions,
                val generator: PlotGenerator) {
    val world: World by lazy {
        val tmp = Bukkit.getWorld(name)
        if (tmp == null) {
            throw NullPointerException("World $name does not appear to be loaded")
        }
        tmp
    }



    fun plotByID(x: Int, z: Int) {

    }

    fun plotByID(id: Vec2i) = plotByID(id.x, id.z)





}