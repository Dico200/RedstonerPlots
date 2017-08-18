package com.redstoner.plots.storage

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.future.future
import org.junit.Test

class CoroutineFutureTest {

    @Test
    fun testCoroutineFuture() {

        val future = future(start = CoroutineStart.ATOMIC) {
            somewhatExpensiveComputation()
        }

        val arrayFuture = future.thenApplyAsync {
            arrayOfNulls<Int>(it)
        }

        val out = future.get()
        val array = arrayFuture.get()

        println(out)
        println(array)
    }

    suspend fun somewhatExpensiveComputation(): Int {
        var out = 0
        for (i in 1..10) {
            out += i
        }
        return out
    }

}