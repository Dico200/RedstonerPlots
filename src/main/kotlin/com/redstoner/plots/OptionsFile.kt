package com.redstoner.plots

import com.google.common.io.Files
import com.redstoner.plots.util.Jackson
import java.io.File
import java.nio.charset.Charset
import kotlin.reflect.KClass

class OptionsFile<T : Any>(private val file: File, private val name: String, private val clazz: KClass<T>, factory: () -> T) {
    var ref: T = factory()
        private set

    init {
        if (file.exists()) {
            load()
        } else {
            save()
        }
    }

    fun load() {

    }

    fun save() {
        Files.newReader(file, Charset.defaultCharset()).use {
            ref = Jackson.yamlObjectMapper.readValue(it, clazz.java)
        }
/*

        try (reader = Files.newReader(file, Charset.defaultCharset())){
            ref = Jackson.yamlObjectMapper.readValue(Files.newReader(file, Charset.defaultCharset()), clazz.java)
        } catch (ex: IOException) {
            onError("saving $name", ex)
        }
        */
    }

    fun onError(action: String, ex: Throwable) {
        println("error occurred while $action: $ex")
        ex.printStackTrace()
    }

}