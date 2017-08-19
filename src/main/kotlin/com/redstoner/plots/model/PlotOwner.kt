package com.redstoner.plots.model

import com.redstoner.plots.util.UUIDUtil
import org.bukkit.Bukkit
import java.util.*

data class PlotOwner(var uuid: UUID? = null,
                     var name: String? = null) {

    init {
        uuid ?: name ?: throw IllegalArgumentException("uuid and/or name must be present")
    }

    val playerName: String get() {
        return UUIDUtil.getPlayerName(uuid ?: return name!!, name ?: ":unknownName:")
    }

    val offlinePlayer get() = if (uuid != null) Bukkit.getOfflinePlayer(uuid) else Bukkit.getOfflinePlayer(name)

}