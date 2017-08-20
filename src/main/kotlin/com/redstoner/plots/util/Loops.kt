package com.redstoner.plots.util

open class LoopException(val isBreak: Boolean) : Exception(null, null, false, false)

class LoopScope {
    var exc: Exception? = null

    fun doBreak(): Nothing {
        exc = LoopException(true)
        throw exc!!
    }

    fun doContinue(): Nothing {
        exc = LoopException(false)
        throw exc!!
    }

}

inline fun loop(block: LoopScope.() -> Unit) {
    val scope = LoopScope()
    while (true) {
        try {
            block(scope)
        } catch (ex: LoopException) {
            if (ex === scope.exc) {
                if (ex.isBreak) break else continue
            }
            throw ex
        }
    }
}