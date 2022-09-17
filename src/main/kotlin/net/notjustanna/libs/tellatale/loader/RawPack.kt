package net.notjustanna.libs.tellatale.loader

import org.luaj.vm2.LuaTable
import net.notjustanna.libs.tellatale.pack.TalePack

data class RawPack(val result: LuaTable) {
    val child = LinkedHashMap<String, RawPhase>()

    fun build() : TalePack {
        return TalePack()
    }
}

data class RawPhase(val result: LuaTable) {
    val child = LinkedHashMap<String, RawStage>()

    fun build() {

    }
}

data class RawStage(val result: LuaTable) {
    val child = LinkedHashMap<String, RawAction>()

    fun build() {

    }
}

data class RawAction(val isNpc: Boolean, val result: LuaTable) {
    fun build() {

    }
}
