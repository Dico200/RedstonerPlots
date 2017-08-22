package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import io.dico.dicore.util.Registrator
import org.bukkit.block.Block
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.InventoryHolder

inline fun Block.toVec2i(): Vec2i = Vec2i(x, z)

inline fun <reified T : Event> Registrator.registerListener(noinline block: T.() -> Unit)
        = registerListener(EventPriority.NORMAL, true, block)

inline fun <reified T : Event> Registrator.registerListener(priority: EventPriority, noinline block: T.() -> Unit)
        = registerListener(T::class.java, priority, block)

inline fun <reified T : Event> Registrator.registerListener(ignoreCancelled: Boolean, noinline block: T.() -> Unit)
        = registerListener(T::class.java, ignoreCancelled, block)

inline fun <reified T : Event> Registrator.registerListener(priority: EventPriority, ignoreCancelled: Boolean, noinline block: T.() -> Unit)
        = registerListener(T::class.java, priority, ignoreCancelled, block)

private fun checkPistonAction(event: BlockPistonEvent, affectedBlocks: List<Block>) {
    val world = getWorld(event.block.world) ?: return
    val direction = event.direction
    val affectedColumns = HashSet<Vec2i>()
    affectedBlocks.forEach {
        affectedColumns += it.toVec2i()
        affectedColumns += it.getRelative(direction).toVec2i()
    }

    affectedColumns.forEach {
        val plot = world.plotAt(it)
        if (plot == null || plot.hasBlockVisitors()) {
            event.isCancelled = true
            return
        }
    }
}

val registerListeners: Registrator.() -> Unit = {
    registerListener<PlayerMoveEvent> h@ {
        val from = this.from
        val to = this.to
        if (from.x.toInt() == to.x.toInt() && from.z.toInt() == to.z.toInt()) return@h
        val user = player
        if (user.hasBanBypass) return@h
        val plot = getPlotAt(user)
        if (plot?.isBanned(user) ?: false) {
            this.to = from
            isCancelled = true
        }
    }
    registerListener<BlockBreakEvent> h@ {
        val user = player
        val world = getWorld(user.world) ?: return@h
        if (!user.hasBuildAnywhere && world.plotAt(user)?.canBuild(user) != true) {
            // not allowed to build
            isCancelled = true
        } else if (!world.options.dropEntityItems) {
            // avoid containers dropping a bunch of items
            val state = block.state
            if (state is InventoryHolder) {
                state.inventory.clear()
                state.update()
            }
        }
    }
    registerListener<BlockPlaceEvent> h@ {
        val user = player
        val world = getWorld(user.world) ?: return@h
        if (!user.hasBuildAnywhere && world.plotAt(user)?.canBuild(user) != true) {
            isCancelled = true
        }
    }
    registerListener<BlockPistonExtendEvent> { checkPistonAction(this, blocks) }
    registerListener<BlockPistonRetractEvent> { checkPistonAction(this, blocks) }
    registerListener<ExplosionPrimeEvent> h@ {
        val loc = entity.location
        val world = getWorld(loc.world) ?: return@h
        if (world.plotAt(loc)?.hasBlockVisitors() == true) {
            isCancelled = true
            radius = 0F
        } else if (world.options.disableExplosions) {
            radius = 0F
        }
    }
    registerListener<EntityExplodeEvent> h@ {
        val loc = entity.location
        val world = getWorld(loc.world) ?: return@h
        if (world.options.disableExplosions || world.plotAt(loc)?.hasBlockVisitors() == true) {
            isCancelled = true
        }
    }
    registerListener<BlockFromToEvent> {
        val plot = getPlotAt(toBlock)
        if (plot == null || plot.hasBlockVisitors()) {
            isCancelled = true
        }
    }
    registerListener<PlayerInteractEvent> h@ {
        val user = player
        val world = getWorld(user.world) ?: return@h

        val hasAdminPerm = user.hasBuildAnywhere
        val clicked = clickedBlock
        val clickedPlot = if (clicked == null) null else world.plotAt(clicked)

        if (clickedPlot?.isBanned(user) == true) {

        }





    }

}
