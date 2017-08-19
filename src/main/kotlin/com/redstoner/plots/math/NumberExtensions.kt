package com.redstoner.plots.math

object NumberExtensions {

    @JvmStatic
    fun floor(d: Double): Int {
        val down = d.toInt()
        if (down.toDouble() != d && (java.lang.Double.doubleToRawLongBits(d).ushr(63).toInt()) == 1) {
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

}