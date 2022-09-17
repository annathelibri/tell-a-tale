package net.notjustanna.libs.tellatale.lua

import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import org.luaj.vm2.LuaBoolean.s_metatable as booleanMetatable
import org.luaj.vm2.LuaFunction.s_metatable as functionMetatable
import org.luaj.vm2.LuaNil.s_metatable as nilMetatable
import org.luaj.vm2.LuaNumber.s_metatable as numberMetatable
import org.luaj.vm2.LuaString.s_metatable as stringMetatable
import org.luaj.vm2.LuaThread.s_metatable as threadMetatable

class LuaManager {
    private val compileGlobals = Globals().apply {
        load(JseBaseLib())
        set("package", LuaTable().apply { set("loaded", LuaTable()) }) //PackageLib replacement
        load(StringLib())
        load(JseMathLib())
        LoadState.install(this)
        LuaC.install(this)
    }

    init {
        booleanMetatable = ReadOnlyLuaTable(booleanMetatable ?: LuaTable())
        functionMetatable = ReadOnlyLuaTable(functionMetatable ?: LuaTable())
        nilMetatable = ReadOnlyLuaTable(nilMetatable ?: LuaTable())
        numberMetatable = ReadOnlyLuaTable(numberMetatable ?: LuaTable())
        stringMetatable = ReadOnlyLuaTable(stringMetatable ?: LuaTable())
        threadMetatable = ReadOnlyLuaTable(threadMetatable ?: LuaTable())
    }

    fun newSandbox(configure: Globals.() -> Unit = {}): Sandbox {
        val debug: LuaValue

        val globals = Globals().apply {
            load(JseBaseLib())
            set("collectgarbage", LuaValue.NIL)
            set("dofile", LuaValue.NIL)
            set("loadfile", LuaValue.NIL)
            set("print", LuaValue.NIL)
            set("package", LuaTable().apply { set("loaded", LuaTable()) }) //PackageLib replacement
            load(Bit32Lib())
            load(TableLib())
            load(StringLib())
            load(JseMathLib())
            load(DebugLib())
            debug = get("debug")

            configure()

            //cleanup
            set("debug", LuaValue.NIL)
            set("package", LuaValue.NIL)
        }

        return Sandbox(globals, debug)
    }

    inner class Sandbox internal constructor(val globals: Globals, private val debugLib: LuaValue) {
        fun execute(script: String, scriptName: String = "script", instructionCount: Int = -1): Varargs {
            return run(compileGlobals.load(script, scriptName, globals), instructionCount)
        }

        fun execute(script: Reader, scriptName: String = "script", instructionCount: Int = -1): Varargs {
            return run(compileGlobals.load(script, scriptName, globals), instructionCount)
        }

        fun execute(script: InputStream, scriptName: String = "script", instructionCount: Int = -1): Varargs {
            return execute(InputStreamReader(script), scriptName, instructionCount)
        }

        fun run(chunk: LuaValue, instructionCount: Int): Varargs {
            val thread = LuaThread(globals, chunk)

            if (instructionCount >= 0) {
                debugLib.get("sethook").invoke(
                    LuaValue.varargsOf(
                        arrayOf(
                            thread,
                            object : ZeroArgFunction() {
                                override fun call(): LuaValue {
                                    throw Error("Script overran resource limits.")
                                }
                            },
                            LuaValue.EMPTYSTRING,
                            LuaValue.valueOf(instructionCount)
                        )
                    )
                )
            }

            return thread.resume(LuaValue.NIL)
        }
    }

    fun execute(script: String, scriptName: String = "script"): Varargs {
        return newSandbox().execute(script, scriptName, 200)
    }

    internal class ReadOnlyLuaTable(table: LuaValue) : LuaTable() {
        init {
            presize(table.length(), 0)
            var n = table.next(LuaValue.NIL)
            while (!n.arg1().isnil()) {
                val key = n.arg1()
                val value = n.arg(2)
                super.rawset(key, if (value.istable()) ReadOnlyLuaTable(value) else value)
                n = table
                    .next(n.arg1())
            }
        }

        override fun setmetatable(metatable: LuaValue?): LuaValue {
            return LuaValue.error("table is read-only")
        }

        override fun set(key: Int, value: LuaValue) {
            LuaValue.error("table is read-only")
        }

        override fun rawset(key: Int, value: LuaValue) {
            LuaValue.error("table is read-only")
        }

        override fun rawset(key: LuaValue, value: LuaValue?) {
            LuaValue.error("table is read-only")
        }

        override fun remove(pos: Int): LuaValue {
            return LuaValue.error("table is read-only")
        }
    }
}