package com.redstoner.plots.model.generator

import com.redstoner.plots.model.PlotWorld
import kotlin.reflect.KClass

interface GeneratorFactory {

    val name: String

    val optionsClass: KClass<out GeneratorOptions>

    fun newPlotGenerator(world: PlotWorld): PlotGenerator

}