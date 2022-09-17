package net.notjustanna.libs.tellatale.utils

import com.grack.nanojson.*

object Json {
    fun parser() = JsonParser.any()

    fun arrayParser() = JsonParser.array()

    fun objectParser() = JsonParser.`object`()

    fun arrayBuilder() = JsonArray.builder()

    fun objectBuilder() = JsonObject.builder()
}