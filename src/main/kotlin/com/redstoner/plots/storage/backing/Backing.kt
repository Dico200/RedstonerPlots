package com.redstoner.plots.storage.backing

import com.redstoner.plots.Plot
import com.redstoner.plots.PlotData
import com.redstoner.plots.PlotOwner
import com.redstoner.plots.storage.SerializablePlot
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.util.*

interface Backing {

    val name: String

    suspend fun init()

    suspend fun shutdown()

    /**
     * This producer function is capable of constantly reading plots from a potentially infinite sequence,
     * and provide plotdata for it as read from the database.
     */
    val producePlotData: suspend ProducerScope<Pair<Plot, PlotData?>>.(Sequence<Plot>) -> Unit

    suspend fun readPlotData(plotFor: Plot): PlotData?

    suspend fun getOwnedPlots(user: PlotOwner): List<SerializablePlot>

    suspend fun setPlotOwner(plotFor: Plot, owner: PlotOwner?)

    suspend fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?)

    suspend fun setPlotAllowsInteractInventory(plot: Plot, value: Boolean)

    suspend fun setPlotAllowsInteractInputs(plot: Plot, value: Boolean)

}