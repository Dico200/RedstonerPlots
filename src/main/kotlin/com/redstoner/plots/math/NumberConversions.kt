package com.redstoner.plots.math

object NumberConversions {

    fun floor(d: Double): Int {
        val down = d.toInt()
        if (down.toDouble() != d && (java.lang.Double.doubleToRawLongBits(d).ushr(63).toInt()) == 1) {
            return down-1
        }
        return down
    }

}