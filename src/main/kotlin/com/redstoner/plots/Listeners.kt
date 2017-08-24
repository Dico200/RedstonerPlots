package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import io.dico.dicore.util.Registrator
import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause.EXPLOSION
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.inventory.InventoryHolder
import kotlin.reflect.KClass

inline fun <reified T : Event> Registrator.registerListener(priority: EventPriority = EventPriority.NORMAL,
                                                            ignoreCancelled: Boolean = true,
                                                            noinline block: T.() -> Unit): Registrator =
        registerListener(T::class.java, priority, ignoreCancelled, block)

inline fun <reified T : Event> Registrator.registerMultiple(priority: EventPriority = EventPriority.NORMAL,
                                                            ignoreCancelled: Boolean = true,
                                                            noinline block: T.() -> Unit,
                                                            vararg eventClasses: KClass<out T>): Registrator {
    for (eventClass in eventClasses) {
        registerListener(eventClass.java, priority, ignoreCancelled, block)
    }
    return this
}

inline fun Block.toVec2i(): Vec2i = Vec2i(x, z)

inline fun Cancellable.cancel() {
    isCancelled = true
}

private val trackedEntities = HashMap<Entity, Plot>()

fun updateTrackedEntities() {
    val iterator = trackedEntities.iterator()
    while (iterator.hasNext()) {
        val (entity, plot) = iterator.next()
        if (entity.isDead || entity.isOnGround) {
            iterator.remove()
            continue
        }
        if (plot.hasBlockVisitors()) {
            iterator.remove()
            continue
        }
        val curPlot = getPlotAt(entity.location)
        if (curPlot !== plot && !curPlot.hasBlockVisitors()) {
            entity.remove()
            iterator.remove()
        }
    }
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
        if (plot === null || plot.hasBlockVisitors()) {
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
        if (plot.isBanned(user)) {
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
        if (plot === null || plot.hasBlockVisitors()) {
            cancel()
        }
    }
    registerListener<PlayerInteractEvent> h@ {
        val user = player
        val world = getWorld(user.world) ?: return@h
        val clicked = clickedBlock
        val clickedPlot = if (clicked === null) null else world.plotAt(clicked)

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
                        if (!hasAdminPerm && clickedPlot !== null && !clickedPlot.allowsInteractInputs && !clickedPlot.canBuild(user)) {
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

                            if (bedHead.type === Material.BED_BLOCK && bedHead.data > 7 && (bedHead.biome === Biome.HELL || bedHead.biome === Biome.SKY)) {
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
                if (!hasAdminPerm && clickedPlot != null && !clickedPlot.allowsInteractInputs && !clickedPlot.canBuild(user)) {
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

    registerListener<EntityChangeBlockEvent> h@ {
        val entity = entity
        val world = getWorld(entity.world) ?: return@h
        val plot = world.plotAt(entity)
        if (entity.type === EntityType.ENDERMAN || plot.hasBlockVisitors()) {
            cancel()
        } else if (plot != null && entity.type === EntityType.FALLING_BLOCK) {
            trackedEntities[entity] = plot
        }
    }

    registerListener<EntityCreatePortalEvent> h@ {
        if (getWorld(entity.world)?.options?.blockPortalCreation == true) {
            cancel()
        }
    }

    registerMultiple<PlayerEvent>(block = h@ {
        val user = player
        if (user.hasBuildAnywhere) return@h
        val world = getWorld(user.world) ?: return@h
        val plot = world.plotAt(user)
        if (plot === null || !(plot.allowsInteractInventory || plot.canBuild(user))) {
            (this as Cancellable).cancel()
        }
    }, eventClasses = *arrayOf(PlayerDropItemEvent::class, PlayerPickupItemEvent::class))

    registerListener<InventoryClickEvent> h@ {
        val user = whoClicked
        if (user !is Player || user.hasBuildAnywhere) return@h
        val world = getWorld(user.world) ?: return@h
        val inventory = inventory ?: return@h // if hotbar, returns null
        val holder = inventory.holder
        if (holder === user) return@h
        val location = inventory.location ?: return@h
        val plot = world.plotAt(location)
        if (plot === null || !(plot.allowsInteractInventory || plot.canBuild(user))) {
            cancel()
        }
    }

    registerListener<WeatherChangeEvent>(ignoreCancelled = false) h@ {
        if (!toWeatherState()) return@h
        val world = getWorld(world) ?: return@h
        if (world.options.noWeather) {
            cancel()
        }
    }

    registerListener<EntitySpawnEvent> h@ {
        val entity = entity
        val world = getWorld(entity.world) ?: return@h
        if (entity is Creature && world.options.blockMobSpawning) {
            cancel()
        } else if (world.plotAt(entity.location).hasBlockVisitors()) {
            cancel()
        }
    }

    registerListener<VehicleMoveEvent>(ignoreCancelled = false) h@ {
        val to = to
        val world = getWorld(to.world) ?: return@h
        val plot = world.plotAt(to)
        if (plot === null) {
            val vehicle = vehicle
            var eject = false
            for (passenger in vehicle.passengers) {
                if (passenger is Player) {
                    eject = true
                    passenger.sendPlotMessage(nopermit = true, message = "Your ride ends here")
                } else {
                    passenger.remove()
                }
            }
            if (eject) {
                vehicle.eject()
            }
            vehicle.remove()
        } else if (plot.hasBlockVisitors()) {
            // attempt to cancel the event
            to.subtract(to).add(from)
        }
    }

    registerListener<EntityDamageByEntityEvent>(ignoreCancelled = false) h@ {
        val victim = entity
        val world = getWorld(victim.world) ?: return@h
        val damager = damager

        if (world.options.disableExplosions && (damager is ExplosiveMinecart || damager is Creeper)) {
            cancel()
            return@h
        }

        val user = when {
            damager is Player -> damager
            damager is Projectile && damager.shooter is Player -> damager.shooter as Player
            else -> return@h
        }

        if (user.hasBuildAnywhere) return@h
        val plot = world.plotAt(victim.location)
        if (plot === null || !plot.canBuild(user)) {
            cancel()
        }
    }

    registerListener<HangingBreakEvent> h@ {
        val location = entity.location
        val world = getWorld(location.world) ?: return@h
        if ((cause === EXPLOSION && world.options.disableExplosions) || world.plotAt(location).hasBlockVisitors()) {
            cancel()
        }
    }

    registerListener<HangingBreakByEntityEvent> h@ {
        val entity = entity
        val world = getWorld(entity.world) ?: return@h
        val remover = remover
        if (remover is Player && !remover.hasBuildAnywhere) {
            val plot = world.plotAt(entity.location)
            if (plot === null || !plot.canBuild(remover)) {
                cancel()
            }
        }
    }

    registerListener<HangingPlaceEvent> h@ {
        val user = player
        if (user.hasBuildAnywhere) return@h
        val world = getWorld(user.world) ?: return@h
        val block = block.getRelative(blockFace)
        val plot = world.plotAt(block)
        if (plot === null || !plot.canBuild(user)) {
            cancel()
        }
    }

    registerListener<StructureGrowEvent> h@ {
        val world = getWorld(location.world) ?: return@h
        val plot = world.plotAt(location)
        if (plot === null) {
            cancel()
            return@h
        }

        val user = player
        if (!user.hasBuildAnywhere && !plot.canBuild(user)) {
            cancel()
            return@h
        }

        blocks.removeIf { world.plotAt(it.block) !== plot }
    }

    registerListener<BlockDispenseEvent>(ignoreCancelled = false) h@ {
        val block = block
        val type = block.type
        if (type !== Material.DISPENSER && type !== Material.DROPPER) return@h
        val world = getWorld(block.world) ?: return@h

        val dispenserFace = when (block.data.toInt()) {
            0, 6, 8, 14 -> BlockFace.DOWN
            1, 7, 9, 15 -> BlockFace.UP
            2, 10 -> BlockFace.NORTH
            3, 11 -> BlockFace.SOUTH
            4, 12 -> BlockFace.WEST
            5, 13 -> BlockFace.EAST
            else -> return@h
        }

        if (world.plotAt(block.getRelative(dispenserFace)) === null) {
            cancel()
        }
    }

    registerListener<ItemSpawnEvent>(priority = EventPriority.HIGHEST) h@ {
        val item = entity
        val world = getWorld(item.world) ?: return@h
        val plot = world.plotAt(item.location)
        if (plot === null) {
            cancel()
        } else {
            trackedEntities[item] = plot
        }
    }

    registerListener<EntityTeleportEvent> h@ {
        val from = from
        val plot = getPlotAt(from) ?: return@h
        if (plot.hasBlockVisitors()) return@h
        if (plot !== plot.world.plotAt(to)) {
            cancel()
        }
    }

    registerListener<ProjectileLaunchEvent> h@ {
        val arrow = entity
        val world = getWorld(arrow.world) ?: return@h
        val plotFrom = world.plotAt(arrow.location)
        if (plotFrom === null || (arrow.shooter is Player && !plotFrom.canBuild(arrow.shooter as Player))) {
            cancel()
        } else {
            trackedEntities[arrow] = plotFrom
        }
    }

    registerListener<EntityDeathEvent>(ignoreCancelled = false) h@ {
        val entity = entity
        trackedEntities.remove(entity)
        val world = getWorld(entity.world) ?: return@h
        if (world.options.dropEntityItems) return@h
        drops.clear()
        droppedExp = 0
    }

    registerListener<PlayerChangedWorldEvent>(ignoreCancelled = false) h@ {
        val user = player
        if (user.hasGamemodeBypass) return@h
        val world = getWorld(user.world) ?: return@h
        val gameMode = world.options.gameMode ?: return@h
        user.gameMode = gameMode
    }

}
