package com.redstoner.plots

import com.fasterxml.jackson.core.JsonProcessingException
import com.google.common.io.Files
import com.redstoner.plots.storage.Storage
import io.dico.dicore.command.CommandBuilder
import io.dico.dicore.command.EOverridePolicy
import io.dico.dicore.util.Registrator
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
    val storage: Storage
    var options: Options
        private set
    private var worldsLoaded = false

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

        storage = options.storage.newStorageInstance()
    }

    fun loadWorldsIfNeeded() {
        if (!worldsLoaded) {
            synchronized(this) {
                if (worldsLoaded) {
                    return
                }
                worldsLoaded = true
            }
            loadWorlds(options)
        }
    }

    override fun onEnable() {
        registerListeners(Registrator(this))

        // there must be one world loaded before we can load worlds, as the creation of new worlds requires a world to be loaded
        // to get the default gamemode. It's completely retarded.
        if (server.worlds.size > 0) {
            loadWorldsIfNeeded()
        }

        CommandBuilder()
                .group("plot", "plots", "p")
                .registerCommands(PlotCommands())
                .parent()
                .getDispatcher().registerToCommandMap("redstonerplots:", EOverridePolicy.OVERRIDE_ALL)
    }

    override fun onDisable() {
        worldsLoaded = false
    }

    fun loadOptions() {
        try {
            Files.newReader(optionsFile, Charset.defaultCharset()).use {
                options.mergeFrom(it)
            }
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
            Files.newWriter(optionsFile, Charset.defaultCharset()).use {
                options.writeTo(it)
            }
        } catch (ex: IOException) {
            logger.severe("failed to save the options: $ex")
            ex.printStackTrace()
        }
    }

    override fun getDefaultWorldGenerator(worldName: String?, id: String?) = getWorld(worldName!!)?.generator

}