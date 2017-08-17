package com.redstoner.plots.util

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.redstoner.plots.model.generator.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

object Jackson {
    val yamlObjectMapper = ObjectMapper(YAMLFactory())

    init {
        yamlObjectMapper.propertyNamingStrategy = PropertyNamingStrategy.KEBAB_CASE

        val kotlinModule = KotlinModule()

        with(kotlinModule) {
            setSerializerModifier(object : BeanSerializerModifier() {
                @Suppress("UNCHECKED_CAST")
                override fun modifySerializer(config: SerializationConfig?, beanDesc: BeanDescription?, serializer: JsonSerializer<*>?): JsonSerializer<*> {
                    if (GeneratorOptions::class.isSuperclassOf(beanDesc?.beanClass?.kotlin as KClass<*>)) {
                        return GeneratorOptionsSerializer(serializer as JsonSerializer<GeneratorOptions>)
                    }

                    return super.modifySerializer(config, beanDesc, serializer)
                }
            })
            addSerializer(BlockTypeSerializer())
            addDeserializer(BlockType::class.java, BlockTypeDeserializer())
            addDeserializer(GeneratorOptions::class.java, GeneratorOptionsDeserializer())
        }

        yamlObjectMapper.registerModule(kotlinModule)
    }






}