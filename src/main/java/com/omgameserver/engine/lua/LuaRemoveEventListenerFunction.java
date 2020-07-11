package com.omgameserver.engine.lua;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaRemoveEventListenerFunction extends TwoArgFunction {
    static private final Logger logger = LoggerFactory.getLogger(LuaRemoveEventListenerFunction.class);

    private final Map<LuaString, Set<LuaFunction>> functional;
    private final Map<LuaString, Set<LuaTable>> tabulated;

    LuaRemoveEventListenerFunction(Map<LuaString, Set<LuaFunction>> functional, Map<LuaString, Set<LuaTable>> tabulated) {
        super();
        this.functional = functional;
        this.tabulated = tabulated;
    }

    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        if (arg1.isstring()) {
            LuaString key = arg1.checkstring();
            if (arg2.isfunction()) {
                Set<LuaFunction> functions = functional.get(key);
                if (functions != null) {
                    functions.remove(arg2.checkfunction());
                }
                logger.trace("Remove functional event listener for {}", key.toString());
                return LuaBoolean.TRUE;
            } else if (arg2.istable()) {
                Set<LuaTable> tables = tabulated.get(key);
                if (tables != null) {
                    tables.remove(arg2.checktable());
                }
                logger.trace("Remove tabulated event listener for {}", key.toString());
                return LuaBoolean.TRUE;
            }
        }
        return LuaBoolean.FALSE;
    }
}