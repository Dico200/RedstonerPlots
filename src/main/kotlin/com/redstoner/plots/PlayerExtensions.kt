package com.redstoner.plots

import com.redstoner.plots.util.toIntOr
import org.bukkit.entity.Player

val Player.hasBanBypass get() = hasPermission("plots.admin.bypass.ban")
val Player.hasBuildAnywhere get() = hasPermission("plots.admin.bypass.build")
val Player.hasGamemodeBypass get() = hasPermission("plots.admin.bypass.gamemode")
val Player.hasAdminManage get() = hasPermission("plots.admin.manage")
val Player.hasPlotHomeOthers get() = hasPermission("plots.command.home.others")
val Player.hasRandomSpecific get() = hasPermission("plots.command.random.specific")
val Player.plotLimit: Int
    get() {
        for (info in effectivePermissions) {
            val perm = info.permission
            if (perm.startsWith("plots.limit.")) {
                val limitString = perm.substring("plots.limit.".length)
                if (limitString == "*") {
                    return Int.MAX_VALUE
                }
                return limitString.toIntOr {
                    Main.instance.logger.severe("$name has permission '$perm'. The suffix can not be parsed to an integer (or *).")
                    DEFAULT_LIMIT
                }
            }
        }
        return DEFAULT_LIMIT
    }

val DEFAULT_LIMIT = 1
