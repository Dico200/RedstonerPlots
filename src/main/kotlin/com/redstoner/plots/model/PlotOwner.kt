package com.redstoner.plots.model

import java.util.*

data class PlotOwner(var uuid: UUID? = null,
                     var name: String? = null) {

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

}