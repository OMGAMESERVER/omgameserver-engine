package com.omgameserver.engine.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LuaLogFunction extends VarArgFunction {
    static private final Logger logger = LoggerFactory.getLogger(LuaLogFunction.class);
    private final LEVEL logLevel;
    private final LuaValue tostring;

    LuaLogFunction(Globals globals, LEVEL logLevel) {
        this.logLevel = logLevel;
        tostring = globals.get("tostring");
    }

    @Override
    public Varargs invoke(Varargs args) {
        StringBuilder result = new StringBuilder();
        for (int i = 1, n = args.narg(); i <= n; i++) {
            if (i > 1) {
                result.append(' ');
            }
            LuaString s = tostring.call(args.arg(i)).strvalue();
            result.append(s.tojstring());
        }
        switch (logLevel) {
            case ERROR:
                logger.error("{}", result.toString());
                break;
            case WARN:
                logger.warn("{}", result.toString());
                break;
            case INFO:
                logger.info("{}", result.toString());
                break;
            case DEBUG:
                logger.debug("{}", result.toString());
                break;
            case TRACE:
                logger.trace("{}", result.toString());
                break;
        }
        return NONE;
    }

    enum LEVEL {
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
    }
}
