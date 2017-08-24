package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import io.dico.dicore.util.Registrator
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.WorldLoadEvent
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

inline fun Cancellable.cancel() {
    isCancelled = true
}

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
            event.cancel()
            return
        }
    }
}

val registerListeners: Registrator.() -> Unit = {
    registerListener<WorldLoadEvent> {
        Main.instance.server.scheduler.runTask(Main.instance) {
            Main.instance.loadWorldsIfNeeded()
        }
    }

    registerListener<PlayerMoveEvent> h@ {
        val from = this.from
        val to = this.to
        if (from.x.toInt() == to.x.toInt() && from.z.toInt() == to.z.toInt()) return@h
        val user = player
        if (user.hasBanBypass) return@h
        val plot = getPlotAt(user)
        if (plot?.isBanned(user) ?: false) {
            this.to = from
            cancel()
        }
    }
    registerListener<BlockBreakEvent> h@ {
        val user = player
        val world = getWorld(user.world) ?: return@h
        if (!user.hasBuildAnywhere && world.plotAt(user)?.canBuild(user) != true) {
            // not allowed to build
            cancel()
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
            cancel()
        }
    }
    registerListener<BlockPistonExtendEvent> { checkPistonAction(this, blocks) }
    registerListener<BlockPistonRetractEvent> { checkPistonAction(this, blocks) }
    registerListener<ExplosionPrimeEvent> h@ {
        val loc = entity.location
        val world = getWorld(loc.world) ?: return@h
        if (world.plotAt(loc)?.hasBlockVisitors() == true) {
            cancel()
            radius = 0F
        } else if (world.options.disableExplosions) {
            radius = 0F
        }
    }
    registerListener<EntityExplodeEvent> h@ {
        val loc = entity.location
        val world = getWorld(loc.world) ?: return@h
        if (world.options.disableExplosions || world.plotAt(loc)?.hasBlockVisitors() == true) {
            cancel()
        }
    }
    registerListener<BlockFromToEvent> {
        val plot = getPlotAt(toBlock)
        if (plot == null || plot.hasBlockVisitors()) {
            cancel()
        }
    }
    registerListener<PlayerInteractEvent> h@ {
        val user = player
        val world = getWorld(user.world) ?: return@h
        val clicked = clickedBlock
        val clickedPlot = if (clicked == null) null else world.plotAt(clicked)

        if (clickedPlot.isBanned(user)) {
            user.sendPlotMessage(nopermit = true, message = "You cannot interact with a plot if you're banned from it")
            cancel()
            return@h
        }

        val hasAdminPerm = user.hasBuildAnywhere

        when (action) {
            Action.RIGHT_CLICK_BLOCK -> {
                val type = clicked.type
                when (type) {
                    Material.DIODE_BLOCK_ON,
                    Material.DIODE_BLOCK_OFF,
                    Material.REDSTONE_COMPARATOR_ON,
                    Material.REDSTONE_COMPARATOR_OFF -> {
                        if (!hasAdminPerm && !clickedPlot.canBuild(user)) {
                            cancel()
                            return@h
                        }
                    }

                    Material.LEVER,
                    Material.STONE_BUTTON,
                    Material.WOOD_BUTTON,
                    Material.FENCE_GATE,
                    Material.WOODEN_DOOR,
                    Material.ANVIL,
                    Material.TRAP_DOOR,
                        //Material.REDSTONE_ORE,
                    Material.TRAPPED_CHEST -> {
                        if (!hasAdminPerm && clickedPlot != null && !clickedPlot.allowsInteractInputs && !clickedPlot.canBuild(user)) {
                            cancel()
                            user.sendPlotMessage(except = true, message = "You cannot use inputs in this plot")
                            return@h
                        }
                    }

                    Material.BED_BLOCK -> {
                        if (world.options.disableExplosions) {
                            val bedHead = when (clicked.data.toInt()) {
                                0, 4 -> clicked.getRelative(BlockFace.SOUTH)
                                1, 5 -> clicked.getRelative(BlockFace.WEST)
                                2, 6 -> clicked.getRelative(BlockFace.NORTH)
                                3, 7 -> clicked.getRelative(BlockFace.EAST)
                                else -> clicked
                            }

                            if (bedHead.type == Material.BED_BLOCK && bedHead.data > 7 && (bedHead.biome == Biome.HELL || bedHead.biome == Biome.SKY)) {
                                cancel()
                                user.sendPlotMessage(except = true, message = "You cannot use this bed because it would explode")
                                return@h
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
            Action.RIGHT_CLICK_AIR -> {
                if (hasItem()) {
                    val itemType = item.type
                    if (itemType in world.options.blockedItems) {
                        user.sendPlotMessage(except = true, message = "That item is disabled in this world")
                        cancel()
                    } else if (!hasAdminPerm && !clickedPlot.canBuild(user) && itemType in
                            arrayOf(Material.LAVA_BUCKET, Material.WATER_BUCKET, Material.BUCKET, Material.FLINT_AND_STEEL)) {
                        cancel()
                    }
                }
            }
            Action.PHYSICAL -> {
                if (!hasAdminPerm && !clickedPlot.allowsInteractInputs && !clickedPlot.canBuild(user)) {
                    cancel()
                }
            }
            else -> {
            }
        }
    }

    registerListener<PlayerInteractEntityEvent> h@ {
        val user = player
        if (user.hasBuildAnywhere) return@h

        val world = getWorld(user.world) ?: return@h
        val plot = world.plotAt(user)
        if (plot.canBuild(user)) return@h

        when (rightClicked.type) {
            EntityType.BOAT,
            EntityType.MINECART,
            EntityType.MINECART_CHEST,
            EntityType.MINECART_COMMAND,
            EntityType.MINECART_FURNACE,
            EntityType.MINECART_HOPPER,
            EntityType.MINECART_MOB_SPAWNER,
            EntityType.MINECART_TNT,

            EntityType.ARMOR_STAND,
            EntityType.PAINTING,
            EntityType.ITEM_FRAME,
            EntityType.LEASH_HITCH,

            EntityType.CHICKEN,
            EntityType.COW,
            EntityType.HORSE,
            EntityType.SHEEP,
            EntityType.VILLAGER,
            EntityType.WOLF -> cancel()
            else -> {
            }
        }
    }



}
