package com.redstoner.plots.storage

import com.redstoner.plots.model.Plot
import com.redstoner.plots.model.PlotData
import com.redstoner.plots.model.PlotOptions
import com.redstoner.plots.model.PlotOwner
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.channels.ProducerJob
import java.util.*
import java.util.concurrent.CompletableFuture

interface Storage {

    val name: String

    val syncDispatcher: CoroutineDispatcher

    val asyncDispatcher: CoroutineDispatcher

    fun readPlotData(plotFor: Plot): CompletableFuture<PlotData>

    fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int): ProducerJob<Pair<Plot, PlotData>>

    fun getOwnedPlots(user: PlotOwner): CompletableFuture<Sequence<PlotID>>


    fun setPlotData(plotFor: Plot, data: PlotData): CompletableFuture<Unit>

    fun setPlotOwner(plotFor: Plot, owner: PlotOwner): CompletableFuture<Unit>

    fun setPlotOptions(plotFor: Plot, options: PlotOptions): CompletableFuture<Unit>

    fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?): CompletableFuture<Unit>

}