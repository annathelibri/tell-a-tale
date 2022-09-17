package net.notjustanna.libs.tellatale.lua

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuaManagerTest {
    @Test
    fun validExecutions() {
        val luaSandbox = LuaManager()
        assertTrue(luaSandbox.execute("return 'foo'").arg1().checkboolean())
        assertTrue(luaSandbox.execute("return ('abc'):len()").arg1().checkboolean())
        assertTrue(luaSandbox.execute("return getmetatable('abc')").arg1().checkboolean())
        assertTrue(luaSandbox.execute("return getmetatable('abc').len").arg1().checkboolean())
        assertTrue(luaSandbox.execute("return getmetatable('abc').__index").arg1().checkboolean())
    }

    @Test
    fun invalidExecutions() {
        val luaSandbox = LuaManager()
        assertFalse(luaSandbox.execute("return setmetatable('abc', {})").arg1().checkboolean())
        assertFalse(luaSandbox.execute("getmetatable('abc').len = function() end").arg1().checkboolean())
        assertFalse(luaSandbox.execute("getmetatable('abc').__index = {}").arg1().checkboolean())
        assertFalse(luaSandbox.execute("getmetatable('abc').__index.x = 1").arg1().checkboolean())
        assertFalse(luaSandbox.execute("while true do end").arg1().checkboolean())
    }
}