package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.OmgsProperties;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class LuaWorker extends Bolt {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorker.class);

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Globals globals;
    private final LuaRuntime luaRuntime;

    LuaWorker(OmgsProperties properties, String luaScript, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        super(luaScript, properties.getQueueSize());
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        globals = JsePlatform.standardGlobals();
        globals.finder = new LuaScriptFinder();
        luaRuntime = new LuaRuntime(globals);
        globals.set("runtime", luaRuntime);
        globals.loadfile(luaScript).call();
    }

    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
    }
}
