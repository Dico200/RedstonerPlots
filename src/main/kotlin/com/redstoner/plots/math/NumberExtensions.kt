package com.redstoner.plots.math

fun Double.floor(): Int {
    val down = toInt()
    if (down.toDouble() != this && (java.lang.Double.doubleToRawLongBits(this).ushr(63).toInt()) == 1) {
        return down-1
    }
    return down
}

infix fun Int.umod(divisor: Int): Int {
    val out = this % divisor
    if (out < 0) {
        return out + divisor
    }
    return out
}

val Int.even: Boolean get() = and(1) == 0

fun IntRange.clamp(min: Int, max: Int): IntRange {
    if (first < min) {
        if (last > max) {
            return IntRange(min, max)
        }
        return IntRange(min, last)
    }
    if (last > max) {
        return IntRange(first, max)
    }
    return this
}
