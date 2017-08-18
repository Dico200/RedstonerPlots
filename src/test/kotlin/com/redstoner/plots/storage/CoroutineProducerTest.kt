package com.redstoner.plots.storage

import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.concurrent.Executors

class CoroutineProducerTest {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

        }

        val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
    }

    @Test
    fun test() {
        runBlocking {
            val start = System.nanoTime()
            var out: Int? = null
            try {
                val job = produce<Int>(dispatcher, 0) {
                    send(produceInt())
                }
                out = job.receive()
            } finally {
                println("job took ${System.nanoTime() - start} nanos")
                println(out)
            }
            Thread.sleep(100)
        }
    }

    suspend fun produceInt(): Int {
        return 10
    }

}