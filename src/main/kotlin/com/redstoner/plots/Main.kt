package com.redstoner.plots

import com.google.common.io.Files
import com.redstoner.plots.model.generator.DefaultPlotGenerator
import com.redstoner.plots.model.generator.GeneratorFactories
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

class Main : JavaPlugin() {
    val optionsFile: File
    var options: Options
        private set

    init {
        GeneratorFactories.registerFactory(DefaultPlotGenerator.Factory)

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

    }

    fun loadOptions() {
        try {
            options = Options.loadFrom(Files.newReader(optionsFile, Charset.defaultCharset()))
        } catch (ex: IOException) {
            logger.severe("error occurred while loading the config: $ex")
            ex.printStackTrace()
        }
    }

    fun saveOptions() {
        try {
            options.writeTo(Files.newWriter(optionsFile, Charset.defaultCharset()))
        } catch (ex: IOException) {
            logger.severe("error occurred while saving the options: $ex")
            ex.printStackTrace()
        }
    }

}