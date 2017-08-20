package com.redstoner.plots.storage.backing

import com.redstoner.plots.Plot
import com.redstoner.plots.PlotData
import com.redstoner.plots.PlotOptions
import com.redstoner.plots.PlotOwner
import com.redstoner.plots.storage.SerializablePlot
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.util.*

interface Backing {

    val name: String

    fun init()

    fun close()

    /**
     * This producer function is capable of constantly reading plots from a potentially infinite sequence,
     * and provide plotdata for it as read from the database.
     */
    val plotDataProducer: suspend ProducerScope<Pair<Plot, PlotData>>.(Sequence<Plot>) -> Unit

    suspend fun readPlotData(plotFor: Plot): PlotData

    suspend fun getOwnedPlots(user: PlotOwner): Sequence<SerializablePlot>


    suspend fun setPlotData(plotFor: Plot, data: PlotData)

    suspend fun setPlotOwner(plotFor: Plot, owner: PlotOwner)

    suspend fun setPlotOptions(plotFor: Plot, options: PlotOptions)

    suspend fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?)

}