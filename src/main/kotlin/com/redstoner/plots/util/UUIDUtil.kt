package com.redstoner.plots.util

import org.bukkit.Bukkit
import java.util.*

fun getPlayerName(uuid: UUID, unknown: String = ":unknownName:"): String {
    val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
    if (offlinePlayer == null || (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore())) {
        return unknown
    }
    return offlinePlayer.name
}
