package net.notjustanna.libs.tellatale.loader

import com.grack.nanojson.JsonObject
import net.notjustanna.libs.tellatale.utils.Comparison
import net.notjustanna.libs.tellatale.utils.Json
import java.io.InputStream

sealed class TaleFile<T> : Comparable<TaleFile<*>> {
    enum class FileType(val regex: Regex) {
        PACK_OPTIONS(Regex("options.json", RegexOption.LITERAL)) {
            override fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*> {
                return PackOptions(Json.objectParser().from(inputStream))
            }
        },
        PACK_DATA(Regex("pack.lua", RegexOption.LITERAL)) {
            override fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*> {
                return PackData(inputStream.reader().readText())
            }
        },
        PHASE_DATA(Regex("phases/(\\w+)\\.lua")) {
            override fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*> {
                return PhaseData(params[1], inputStream.reader().readText())
            }
        },
        STAGE_DATA(Regex("phases/(\\w+)/(\\w+)\\.lua")) {
            override fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*> {
                return StageData(params[1], params[2], inputStream.reader().readText())
            }
        },
        NPC_ACTION(Regex("phases/(\\w+)/(\\w+)/npc/(\\w+)\\.lua")) {
            override fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*> {
                return Action(this, params[1], params[2], params[3], inputStream.reader().readText())
            }
        },
        PLAYER_ACTION(Regex("phases/(\\w+)/(\\w+)/player/(\\w+)\\.lua")) {
            override fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*> {
                return Action(this, params[1], params[2], params[3], inputStream.reader().readText())
            }
        };

        abstract fun readFile(params: List<String>, inputStream: InputStream): TaleFile<*>

        companion object {
            fun sequence() = values().asSequence()
        }
    }

    abstract val type: FileType
    abstract val contents: T

    override fun compareTo(other: TaleFile<*>): Int {
        return Comparison.of(this.type, other.type).end()
    }

    data class PackOptions(
        override val contents: JsonObject
    ) : TaleFile<JsonObject>() {
        override val type = FileType.PACK_OPTIONS
    }

    data class PackData(
        override val contents: String
    ) : TaleFile<String>() {
        override val type = FileType.PACK_DATA
    }

    data class PhaseData(
        val phaseName: String,
        override val contents: String
    ) : TaleFile<String>() {
        override val type = FileType.PHASE_DATA

        override fun compareTo(other: TaleFile<*>): Int {
            return Comparison.of(this.type, other.type)
                .thenCompare(this, other, PhaseData::phaseName)
                .end()
        }
    }

    data class StageData(
        val phaseName: String,
        val subPhaseName: String,
        override val contents: String
    ) : TaleFile<String>() {
        override val type = FileType.STAGE_DATA

        override fun compareTo(other: TaleFile<*>): Int {
            return Comparison.of(this.type, other.type)
                .thenCompare(this, other, StageData::phaseName, StageData::subPhaseName)
                .end()
        }
    }

    data class Action(
        override val type: FileType,
        val phaseName: String,
        val subPhaseName: String,
        val actionName: String,
        override val contents: String
    ) : TaleFile<String>() {

        override fun compareTo(other: TaleFile<*>): Int {
            return Comparison.of(this.type, other.type)
                .thenCompare(this, other, Action::phaseName, Action::subPhaseName, Action::actionName)
                .end()
        }
    }
}