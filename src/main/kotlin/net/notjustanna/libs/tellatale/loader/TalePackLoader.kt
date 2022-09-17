package net.notjustanna.libs.tellatale.loader

import com.grack.nanojson.JsonObject
import org.luaj.vm2.LuaTable
import net.notjustanna.libs.tellatale.loader.TaleFile.FileType
import net.notjustanna.libs.tellatale.lua.LuaManager
import net.notjustanna.libs.tellatale.pack.TalePack
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TalePackLoader {
    private val luaManager = LuaManager()

    fun readPack(inputStream: InputStream): TalePack {
        return runCatching { readEntries(inputStream) }
            .getOrElse { throw LoaderException("Couldn't read entries", it) }
            .runCatching { executeEntries(this) }
            .getOrElse { throw LoaderException("Couldn't load entries", it) }
            .runCatching { build() }
            .getOrElse { throw LoaderException("Couldn't build pack", it) }
    }

    private fun readEntries(inputStream: InputStream): List<TaleFile<*>> {
        val zip = ZipInputStream(inputStream)
        val zipFileContents = object : FilterInputStream(zip) {}

        return generateSequence(zip::getNextEntry)
            .mapNotNull { entry ->
                try {
                    entry.takeUnless(ZipEntry::isDirectory)
                        ?.let {
                            FileType.sequence()
                                .mapNotNull { it.regex.matchEntire(entry.name)?.to(it) }
                                .firstOrNull()
                        }
                        ?.let { (result, fileType) -> fileType.readFile(result.groupValues, zipFileContents) }
                } finally {
                    zip.closeEntry()
                }
            }
            .sorted()
            .toList()
    }

    private fun executeEntries(entries: List<TaleFile<*>>): RawPack {
        var options: JsonObject? = null
        var pack: RawPack? = null

        val packGlobals = LuaTable()

        for (entry in entries) {
            @Suppress("NAME_SHADOWING")
            when (entry) {
                is TaleFile.PackOptions -> {
                    assert(pack == null) { "Pack data already loaded! Bad sorting?" }
                    check(options == null) { "Pack options was already loaded! Duplicate file?" }
                    options = entry.contents
                }
                is TaleFile.PackData -> {
                    val (contents) = entry
                    check(pack == null) { "Pack data already loaded! Duplicate file?" }

                    val sandbox = luaManager.newSandbox {
                        set("globals", packGlobals)
                        set("pack", LuaTable())
                    }

                    sandbox.execute(contents, "pack.lua")

                    pack = RawPack(sandbox.globals["pack"].checktable())
                }
                is TaleFile.PhaseData -> {
                    val pack = pack
                    val (phaseName, contents) = entry

                    checkNotNull(pack) { "Pack data not loaded! Missing pack.lua?" }
                    check(!pack.child.contains(phaseName)) {
                        "$phaseName.lua already loaded! Duplicate file?"
                    }

                    val sandbox = luaManager.newSandbox {
                        set("globals", packGlobals)
                        set("pack", LuaManager.ReadOnlyLuaTable(pack.result))
                        set("phase", LuaTable())
                    }

                    sandbox.execute(contents, "phases/$phaseName.lua")

                    pack.child[phaseName] = RawPhase(sandbox.globals["phase"].checktable())
                }
                is TaleFile.StageData -> {
                    val pack = pack
                    val (phaseName, stageName, contents) = entry

                    checkNotNull(pack) { "Pack data not loaded! Missing pack.lua?" }
                    val phase = pack.child[phaseName]
                    checkNotNull(phase) { "Phase data not loaded! Missing $phaseName.lua?" }
                    check(!phase.child.contains(stageName)) {
                        "$stageName.lua already loaded! Duplicate file?"
                    }

                    val sandbox = luaManager.newSandbox {
                        set("globals", packGlobals)
                        set("pack", LuaManager.ReadOnlyLuaTable(pack.result))
                        set("phase", LuaManager.ReadOnlyLuaTable(phase.result))
                        set("stage", LuaTable())
                    }

                    sandbox.execute(contents, "phases/$phaseName/$stageName.lua")

                    phase.child[phaseName] = RawStage(sandbox.globals["stage"].checktable())
                }
                is TaleFile.Action -> {
                    val pack = pack
                    val (type, phaseName, stageName, actionName, contents) = entry
                    val isNpc = type == FileType.NPC_ACTION

                    checkNotNull(pack) { "Pack data not loaded! Missing pack.lua?" }
                    val phase = pack.child[phaseName]
                    checkNotNull(phase) { "Phase data not loaded! Missing $phaseName.lua?" }
                    val stage = phase.child[stageName]
                    checkNotNull(stage) { "Stage data not loaded! Missing $stageName.lua?" }

                    check(!stage.child.contains(actionName)) {
                        "$actionName.lua already loaded! Duplicate file?"
                    }

                    val sandbox = luaManager.newSandbox {
                        set("globals", packGlobals)
                        set("pack", LuaManager.ReadOnlyLuaTable(pack.result))
                        set("phase", LuaManager.ReadOnlyLuaTable(phase.result))
                        set("stage", LuaManager.ReadOnlyLuaTable(stage.result))
                        set("action", LuaTable())
                    }

                    sandbox.execute(
                        contents,
                        "phases/$phaseName/$stageName/${if (isNpc) "npc" else "player"}/$actionName.lua"
                    )

                    stage.child[phaseName] = RawAction(isNpc, sandbox.globals["action"].checktable())
                }
            }
        }

        checkNotNull(pack) { "Pack data not loaded! Missing pack.lua?" }

        return pack
    }
}