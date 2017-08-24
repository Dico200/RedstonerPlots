package com.redstoner.plots

import io.dico.dicore.command.CommandException
import io.dico.dicore.command.annotation.Cmd
import org.bukkit.Bukkit
import org.bukkit.entity.Player


class PlotCommands {

    @Cmd("reloadoptions")
    fun reloadOptions() {
        Main.instance.loadOptions()
    }

    @Cmd("tpworld")
    fun tpWorld(sender: Player, worldName: String): String {
        if (worldName == "list") {
            return Bukkit.getWorlds().joinToString("\n- ", "- ", "")
        }
        val world = Bukkit.getWorld(worldName) ?: throw CommandException("World $worldName is not loaded")
        sender.teleport(world.spawnLocation)
        return "Teleported you to $worldName spawn"
    }

}