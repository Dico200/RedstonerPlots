package com.redstoner.plots.storage.backing

import com.redstoner.plots.model.Plot
import com.redstoner.plots.model.PlotData
import com.redstoner.plots.model.PlotOptions
import com.redstoner.plots.model.PlotOwner
import com.redstoner.plots.storage.PlotID
import kotlinx.coroutines.experimental.channels.ProducerScope
import java.util.*

class MySqlBacking : Backing {

    override val name get() = "MySQL"

    override val plotDataProducer: suspend ProducerScope<Pair<Plot, PlotData>>.(Sequence<Plot>) -> Unit
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    suspend override fun readPlotData(plotFor: Plot): PlotData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun getOwnedPlots(user: PlotOwner): Sequence<PlotID> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotData(plotFor: Plot, data: PlotData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotOwner(plotFor: Plot, owner: PlotOwner) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotOptions(plotFor: Plot, options: PlotOptions) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun setPlotPlayerState(plotFor: Plot, player: UUID, state: Boolean?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
