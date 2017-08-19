package com.redstoner.plots.storage

import com.redstoner.plots.Plot
import com.redstoner.plots.PlotData
import com.redstoner.plots.PlotOptions
import com.redstoner.plots.PlotOwner
import com.redstoner.plots.storage.backing.Backing
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.channels.produce
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

interface Storage {

    val name: String

    val syncDispatcher: CoroutineDispatcher

    val asyncDispatcher: CoroutineDispatcher

    fun readPlotData(plotFor: Plot): CompletableFuture<PlotData>

    fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int): ProducerJob<Pair<Plot, PlotData>>

    fun getOwnedPlots(user: PlotOwner): CompletableFuture<Sequence<SerializablePlot>>


    fun setPlotData(plotFor: Plot, data: PlotData): CompletableFuture<Unit>

    fun setPlotOwner(plotFor: Plot, owner: PlotOwner): CompletableFuture<Unit>

    fun setPlotOptions(plotFor: Plot, options: PlotOptions): CompletableFuture<Unit>

    fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?): CompletableFuture<Unit>

}

class AbstractStorage internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    override val asyncDispatcher = Executors.newFixedThreadPool(4) { Thread(it, "AbstractStorageThread") }.asCoroutineDispatcher()

    private fun <T> future(block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.experimental.future.future(asyncDispatcher, CoroutineStart.ATOMIC, block)

    override fun readPlotData(plotFor: Plot) = future { backing.readPlotData(plotFor) }

    override fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int) =
            produce<Pair<Plot, PlotData>>(asyncDispatcher, capacity = channelCapacity) { backing.plotDataProducer(this, plotsFor) }

    override fun getOwnedPlots(user: PlotOwner) = future { backing.getOwnedPlots(user) }

    override fun setPlotData(plotFor: Plot, data: PlotData) = future { backing.setPlotData(plotFor, data) }

    override fun setPlotOwner(plotFor: Plot, owner: PlotOwner) = future { backing.setPlotOwner(plotFor, owner) }

    override fun setPlotOptions(plotFor: Plot, options: PlotOptions) = future { backing.setPlotOptions(plotFor, options) }

    override fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?) = future { backing.setPlotPlayerState(plotFor, player, state) }

}
