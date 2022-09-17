package net.notjustanna.libs.tellatale.utils

enum class Comparison {
    LT, EQ, GT;

    fun end() = ordinal - 1


    inline fun then(next: () -> Int): Comparison {
        if (this != EQ) return this
        return values()[next().coerceIn(RANGE) + 1]
    }

    inline fun <reified T : Any> thenCast(other: Any, next: (T) -> Int): Comparison {
        if (this != EQ) return this
        return values()[next(other as T).coerceIn(RANGE) + 1]
    }

    inline fun <reified T : Any> thenCompare(thisObj: T, other: Any, vararg next: (T) -> Comparable<*>): Comparison {
        if (this != EQ) return this
        for (function in next) {
            @Suppress("UNCHECKED_CAST")
            val i = (function(thisObj) as Comparable<Any>).compareTo(function(other as T) as Comparable<Any>)
            if (i != 0) return values()[i.coerceIn(RANGE) + 1]
        }
        return EQ
    }

    fun <T : Comparable<T>> then(obj1: T, obj2: T): Comparison {
        if (this != EQ) return this
        return values()[obj1.compareTo(obj2).coerceIn(RANGE) + 1]
    }

    companion object {
        val RANGE = -1..1


        fun of(first: () -> Int): Comparison {
            return values()[first().coerceIn(RANGE) + 1]
        }

        fun <T : Comparable<T>> of(obj1: T, obj2: T): Comparison {
            return values()[obj1.compareTo(obj2).coerceIn(RANGE) + 1]
        }

        fun <T : Any> compare(thisObj: T, other: T, vararg next: (T, T) -> Int): Comparison {
            for (function in next) {
                val i = function(thisObj, other)
                if (i != 0) return values()[i.coerceIn(RANGE) + 1]
            }
            return EQ
        }
    }
}