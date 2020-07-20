package com.omgameserver.engine.lua.runtime;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaEventListener extends LuaTable {

    private final String FUNCTION_ADD_EVENT_LISTENER = "add_event_listener";
    private final String FUNCTION_REMOVE_EVENT_LISTENER = "remove_event_listener";

    private final Map<LuaString, Set<LuaFunction>> functional;
    private final Map<LuaString, Set<LuaTable>> tabulated;

    LuaEventListener() {
        functional = new HashMap<>();
        tabulated = new HashMap<>();
        set(FUNCTION_ADD_EVENT_LISTENER, new LuaAddEventListenerFunction(functional, tabulated));
        set(FUNCTION_REMOVE_EVENT_LISTENER, new LuaRemoveEventListenerFunction(functional, tabulated));
    }

    void dispatch(String id, LuaValue event) {
        LuaString key = LuaString.valueOf(id);
        Set<LuaFunction> functions = functional.get(key);
        if (functions != null) {
            for (LuaFunction luaFunction : functions) {
                luaFunction.call(event);
            }
        }
        Set<LuaTable> tables = tabulated.get(key);
        if (tables != null) {
            for (LuaTable luaTable : tables) {
                LuaValue tableFunction = luaTable.get(key);
                tableFunction.call(luaTable, event);
            }
        }
    }
}
