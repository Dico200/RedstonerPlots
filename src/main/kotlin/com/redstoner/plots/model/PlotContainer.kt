package com.redstoner.plots.model

abstract class PlotContainer {

    abstract fun plotAt(x: Int, z: Int): Plot?

}