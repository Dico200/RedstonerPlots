package com.redstoner.plots.util

inline fun String.toIntOr(block: (String) -> Int): Int = toIntOrNull() ?: block(this)

fun String.substringFrom(idx: Int): String = substring(idx, length)

fun Any?.equalsNullable(other: Any?): Boolean = this?.equals(other) ?: other == null





