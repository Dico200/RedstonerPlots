package com.redstoner.plots

import io.dico.dicore.command.annotation.Cmd

class PlotCommands {

    @Cmd("reloadoptions")
    fun reloadOptions() {
        Main.instance.loadOptions()
    }

}