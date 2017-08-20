package com.redstoner.plots.storage.backing

inline fun <T : AutoCloseable, R> T?.use(block: T.() -> R): R {
    if (this == null) {
        throw NullPointerException()
    }
    var exc: Exception? = null
    try {
        return block(this)
    } catch (blockException: Exception) {
        exc = blockException
        throw exc
    } finally {
        suppressAndClose(exc, true)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun AutoCloseable?.suppressAndClose(priorThrownException: Throwable?, throwResult: Boolean): Throwable? {
    if (this != null) {
        try {
            this.close()
        } catch (exc: Exception) {
            if (priorThrownException != null) {
                exc.addSuppressed(priorThrownException)
            }
            if (throwResult) {
                throw exc
            }
            return exc
        }
    }
    if (priorThrownException != null && throwResult) {
        throw priorThrownException
    }
    return priorThrownException
}

/*
inline fun <T : Connection?> T.use(block: T.() -> Unit): Throwable? {
    var exc: Throwable? = null
    try {
        block(this)
    } catch (blockException: Throwable) {
        exc = blockException
    } finally {
        return suppressAndClose(exc, false)
    }
}

inline infix fun <reified T : Throwable?> T.catch(block: (T) -> Unit): Throwable? {
    if (this != null && T::class.isInstance(this)) {
        block(this)
        return null
    }
    return this
}

inline fun <T: Throwable?> T.finalize() {
    if (this != null) {
        throw this
    }
}*/