package com.redstoner.plots.util

import org.bukkit.Bukkit
import java.nio.ByteBuffer
import java.util.*

fun getPlayerName(uuid: UUID, unknown: String = ":unknownName:"): String {
    val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
    if (offlinePlayer == null || (!offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore())) {
        return unknown
    }
    return offlinePlayer.name
}

fun UUID?.toByteArray(): ByteArray? {
    this ?: return null
    val buf = ByteBuffer.allocate(16)
    buf.putLong(mostSignificantBits)
    buf.putLong(leastSignificantBits)
    return buf.array()
}

fun ByteArray?.toUUID(): UUID? {
    this ?: return null
    val buf = ByteBuffer.wrap(this)
    return UUID(buf.long, buf.long)
}
