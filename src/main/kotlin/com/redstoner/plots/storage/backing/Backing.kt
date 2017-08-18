package com.redstoner.plots.storage.backing

import com.redstoner.plots.model.Plot
import com.redstoner.plots.model.PlotData
import com.redstoner.plots.model.PlotOptions
import com.redstoner.plots.model.PlotOwner
import com.redstoner.plots.storage.PlotID
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.util.*

interface Backing {

    val name: String


    /**
     * This producer function is capable of constantly reading plots from a potentially infinite sequence,
     * and provide plotdata for it as read from the database.
     */
    val plotDataProducer: suspend ProducerScope<Pair<Plot, PlotData>>.(Sequence<Plot>) -> Unit

    suspend fun readPlotData(plotFor: Plot): PlotData

    suspend fun getOwnedPlots(user: PlotOwner): Sequence<PlotID>


    suspend fun setPlotData(plotFor: Plot, data: PlotData)

    suspend fun setPlotOwner(plotFor: Plot, owner: PlotOwner)

    suspend fun setPlotOptions(plotFor: Plot, options: PlotOptions)

    suspend fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?)

}