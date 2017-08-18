package com.redstoner.plots.model

/**
 * Encompasses the data of a plot,
 * without accompanying the plot's location
 */
data class PlotData(var owner: PlotOwner? = null,
               var options: PlotOptions = PlotOptions(),
               var added: PlotAdded = PlotAdded()) {

    fun equalsDefaultData(): Boolean {
        return this == DEFAULT
    }

    private companion object {
        val DEFAULT = PlotData()
    }

}