package com.omgameserver.engine.lua;

import com.omgameserver.engine.OmgsDispatcher;
import org.luaj.vm2.Globals;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaRuntime extends LuaEventListener {

    private final String FUNCTION_LOG_ERROR = "log_error";
    private final String FUNCTION_LOG_WARN = "log_warn";
    private final String FUNCTION_LOG_INFO = "log_info";
    private final String FUNCTION_LOG_DEBUG = "log_debug";
    private final String FUNCTION_LOG_TRACE = "log_trace";

    private final String FUNCTION_SEND = "send";

    LuaRuntime(OmgsDispatcher dispatcher, Globals globals) {
        super();
        set(FUNCTION_LOG_ERROR, new LuaLogFunction(globals, LuaLogFunction.LEVEL.ERROR));
        set(FUNCTION_LOG_WARN, new LuaLogFunction(globals, LuaLogFunction.LEVEL.WARN));
        set(FUNCTION_LOG_INFO, new LuaLogFunction(globals, LuaLogFunction.LEVEL.INFO));
        set(FUNCTION_LOG_DEBUG, new LuaLogFunction(globals, LuaLogFunction.LEVEL.DEBUG));
        set(FUNCTION_LOG_TRACE, new LuaLogFunction(globals, LuaLogFunction.LEVEL.TRACE));
        set(FUNCTION_SEND, new LuaSendFunction(dispatcher));
    }
}
