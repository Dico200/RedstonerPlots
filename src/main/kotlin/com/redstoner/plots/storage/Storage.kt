package com.redstoner.plots.storage

import com.redstoner.plots.DataConnectionOptions
import com.redstoner.plots.Plot
import com.redstoner.plots.PlotData
import com.redstoner.plots.PlotOwner
import com.redstoner.plots.storage.backing.Backing
import com.redstoner.plots.storage.backing.MySqlDriver
import com.redstoner.plots.storage.backing.SqlBacking
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
import kotlin.reflect.KClass

interface Storage {

    val name: String

    val syncDispatcher: CoroutineDispatcher

    val asyncDispatcher: CoroutineDispatcher

    fun init(): CompletableFuture<Unit>

    fun shutdown(): CompletableFuture<Unit>

    fun readPlotData(plotFor: Plot): CompletableFuture<PlotData?>

    fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int): ProducerJob<Pair<Plot, PlotData?>>

    fun getOwnedPlots(user: PlotOwner): CompletableFuture<List<SerializablePlot>>

    fun setPlotOwner(plotFor: Plot, owner: PlotOwner?): CompletableFuture<Unit>

    fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?): CompletableFuture<Unit>

    fun setPlotAllowsInteractInventory(plot: Plot, value: Boolean): CompletableFuture<Unit>

    fun setPlotAllowsInteractInputs(plot: Plot, value: Boolean): CompletableFuture<Unit>

}

class AbstractStorage internal constructor(val backing: Backing) : Storage {
    override val name get() = backing.name
    override val syncDispatcher = Executor { it.run() }.asCoroutineDispatcher()
    override val asyncDispatcher = Executors.newFixedThreadPool(4) { Thread(it, "AbstractStorageThread") }.asCoroutineDispatcher()

    private fun <T> future(block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.experimental.future.future(asyncDispatcher, CoroutineStart.ATOMIC, block)

    override fun init(): CompletableFuture<Unit> = future { backing.init() }

    override fun shutdown(): CompletableFuture<Unit> = future { backing.shutdown() }

    override fun readPlotData(plotFor: Plot) = future { backing.readPlotData(plotFor) }

    override fun readPlotData(plotsFor: Sequence<Plot>, channelCapacity: Int) =
            produce<Pair<Plot, PlotData?>>(asyncDispatcher, capacity = channelCapacity) { backing.producePlotData(this, plotsFor) }

    override fun getOwnedPlots(user: PlotOwner) = future { backing.getOwnedPlots(user) }

    override fun setPlotOwner(plotFor: Plot, owner: PlotOwner?) = future { backing.setPlotOwner(plotFor, owner) }

    override fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?) = future { backing.setPlotPlayerState(plotFor, player, state) }

    override fun setPlotAllowsInteractInventory(plot: Plot, value: Boolean) = future { backing.setPlotAllowsInteractInventory(plot, value) }

    override fun setPlotAllowsInteractInputs(plot: Plot, value: Boolean) = future { backing.setPlotAllowsInteractInputs(plot, value) }
}

interface StorageFactory {
    companion object StorageFactories {
        private val map: MutableMap<String, StorageFactory> = HashMap()

        fun registerFactory(method: String, generator: StorageFactory): Boolean = map.putIfAbsent(method.toLowerCase(), generator) == null

        fun getFactory(method: String): StorageFactory? = map[method.toLowerCase()]

        init {
            // have to write the code like this in kotlin.
            // This code is absolutely disgusting
            ConnectionStorageFactory().register(this)
        }
    }

    val optionsClass: KClass<out Any>

    fun newStorageInstance(method: String, options: Any): Storage

}

class ConnectionStorageFactory : StorageFactory {
    override val optionsClass = DataConnectionOptions::class

    private val types: Map<String, String> = with(HashMap<String, String>()) {
        put("mysql", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource")
        this
    }

    fun register(companion: StorageFactory.StorageFactories) {
        types.keys.forEach {
            companion.registerFactory(it, this)
        }
    }

    override fun newStorageInstance(method: String, options: Any): Storage {
        val driverClass = types.get(method.toLowerCase()) ?: throw IllegalArgumentException("Storage method $method is not supported")
        return AbstractStorage(SqlBacking(MySqlDriver(method.toLowerCase(), options as DataConnectionOptions, driverClass = driverClass)))
    }

}
