package com.redstoner.plots.model.generator

object GeneratorFactories {
    private val map: MutableMap<String, GeneratorFactory> = HashMap()

    fun registerFactory(generator: GeneratorFactory): Boolean = map.putIfAbsent(generator.name, generator) == null

    fun getFactory(name: String): GeneratorFactory? = map.get(name)

}