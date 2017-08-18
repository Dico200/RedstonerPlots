package com.redstoner.plots.model

import java.util.*

class PlotAdded {
    private val _map: MutableMap<UUID, Boolean> = HashMap()

    val map: Map<UUID, Boolean> get() = _map

    fun isAllowed(uuid: UUID): Boolean = _map.getOrDefault(uuid, false)

    fun isBanned(uuid: UUID): Boolean = !_map.getOrDefault(uuid, true)

    operator fun get(uuid: UUID): Boolean? = _map.get(uuid)

    fun ban(uuid: UUID) = setState(uuid, false)

    fun allow(uuid: UUID) = setState(uuid, true)

    fun setState(uuid: UUID, state: Boolean?) {
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