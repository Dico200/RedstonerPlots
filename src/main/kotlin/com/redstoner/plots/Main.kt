package com.redstoner.plots

import com.fasterxml.jackson.core.JsonProcessingException
import com.google.common.io.Files
import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.EOverridePolicy
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

class Main : JavaPlugin() {
    companion object {
        @JvmStatic
        val instance: Main
            get() = _instance!!

        private var _instance: Main? = null
    }

    val optionsFile: File
    var options: Options
        private set

    init {
        _instance = this

        val file = File(dataFolder, "options.yml")
        optionsFile = file
        options = Options()

        if (file.exists()) {
            loadOptions()
        } else {
            val parent = file.parentFile
            if (parent == null || !(parent.exists() || parent.mkdirs()) || !file.createNewFile()) {
                logger.severe("Failed to create file options.yml")
            } else {
                options.addDefaultWorld()
                saveOptions()
            }
        }
    }

    override fun onEnable() {
        loadWorlds(options)

        CommandBuilder()
                .group("plot", "plots", "p")
                .registerCommands(PlotCommands())
                .parent()
                .getDispatcher().registerToCommandMap("redstonerplots:", EOverridePolicy.OVERRIDE_ALL)
    }

    fun loadOptions() {
        try {
            options.mergeFrom(Files.newReader(optionsFile, Charset.defaultCharset()))
            println("Config: $options")
        } catch (ex: JsonProcessingException) {
            logger.severe("there's a syntax error in the options file: ${ex.message}")
            if (optionsFile.renameTo(File(dataFolder, "options-invalid.yml"))) {
                if (options.worlds.isEmpty()) {
                    options.addDefaultWorld()
                }
                saveOptions()
                logger.info("The options file has been renamed to options-invalid.yml, and has been replaced with a properly formatted options file.")
            }
        } catch (ex: IOException) {
            logger.severe("failed to load the options: $ex")
            ex.printStackTrace()
        }
    }

    fun saveOptions() {
        try {
            options.writeTo(Files.newWriter(optionsFile, Charset.defaultCharset()))
        } catch (ex: IOException) {
            logger.severe("failed to save the options: $ex")
            ex.printStackTrace()
        }
    }

}