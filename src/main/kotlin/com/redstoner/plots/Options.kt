package com.redstoner.plots

import com.redstoner.plots.model.WorldOptions
import com.redstoner.plots.util.Jackson
import java.io.Reader
import java.io.Writer

class Options {
    var worlds: Map<String, WorldOptions> = HashMap()
        private set

    fun addWorld(name: String, options: WorldOptions) = (worlds as MutableMap).put(name, options)

    fun addDefaultWorld() = addWorld("plotworld", WorldOptions())

    fun writeTo(writer: Writer) = Jackson.yamlObjectMapper.writeValue(writer, this)

    companion object {
        fun loadFrom(reader: Reader): Options = Jackson.yamlObjectMapper.readValue(reader, Options::class.java)
    }

}