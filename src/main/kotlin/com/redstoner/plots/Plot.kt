package com.redstoner.plots

import com.redstoner.plots.math.Vec2i
import com.redstoner.plots.util.getPlayerName
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class Plot(val world: PlotWorld,
           val coord: Vec2i,
           var data: PlotData? = null) {
    val id get() = "${coord.x}:${coord.z}"

    val isLoaded: Boolean get() = data !== null

    override fun equals(other: Any?): Boolean = (this === other) || (other is Plot && world == other.world && coord == other.coord)

    override fun hashCode(): Int = world.hashCode() + 31 * coord.hashCode()

    override fun toString(): String = "Plot(world=$world, coord=$coord)"

    fun canBuild(player: Player): Boolean {
        val data = this.data ?: return false
        return data.added.isAllowed(player.uniqueId) || data.owner?.matches(player) ?: false
    }

    fun isBanned(player: Player): Boolean {
        val data = this.data ?: return false
        return data.added.isBanned(player.uniqueId)
    }

    fun hasBlockVisitors(): Boolean = false

}

class PlotAdded {
    private val _map: MutableMap<UUID, Boolean> = HashMap()

    val map: Map<UUID, Boolean> get() = _map

    fun isAllowed(uuid: UUID): Boolean = _map.getOrDefault(uuid, false)

    fun isBanned(uuid: UUID): Boolean = !_map.getOrDefault(uuid, true)

    operator fun get(uuid: UUID): Boolean? = _map.get(uuid)

    fun ban(uuid: UUID) = set(uuid, false)

    fun allow(uuid: UUID) = set(uuid, true)

    operator fun set(uuid: UUID, state: Boolean?) {
        synchronized(this) {
            if (state == null) {
                _map.remove(uuid)
            } else {
                _map.put(uuid, state)
            }
        }
    }

    fun getBannedPlayers(): Collection<UUID> = synchronized(this) {
        _map.filterValues { !it }.keys
    }

    fun getAllowedPlayers(): Collection<UUID> = synchronized(this) {
        _map.filterValues { it }.keys
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlotAdded) return false

        if (_map != other._map) return false

        return true
    }

    override fun hashCode(): Int {
        return _map.hashCode()
    }

}

/**
 * Encompasses the data of a plot,
 * without accompanying the plot's location
 */
data class PlotData(var owner: PlotOwner? = null,
                    var options: PlotOptions = PlotOptions(),
                    var added: PlotAdded = PlotAdded()) {

    fun equalsDefaultData(): Boolean {
        return this == DEFAULT
    }

    private companion object {
        val DEFAULT = PlotData()
    }

}

data class PlotOwner(var uuid: UUID? = null,
                     var name: String? = null) {

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val playerName: String
        get() {
            return getPlayerName(uuid ?: return name!!, name ?: ":unknownName:")
        }

    val offlinePlayer get() = if (uuid != null) Bukkit.getOfflinePlayer(uuid) else Bukkit.getOfflinePlayer(name)

    fun matches(player: Player, allowNameMatch: Boolean = false): Boolean {
        return player.uniqueId == uuid || (allowNameMatch && player.name == name)
    }

}
