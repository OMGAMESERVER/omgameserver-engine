package com.omgameserver.engine.lua;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LuaProperties {

    private final int queueSize;
    private final int payloadSize;
    private final String mainScript;

    LuaProperties(@Value("${omgameserver.engine.lua.queueSize:128}") int queueSize,
                  @Value("${omgameserver.engine.lua.payloadSize:1024}") int payloadSize,
                  @Value("${omgameserver.engine.lua.mainScript:main.lua}") String mainScript) {
        this.queueSize = queueSize;
        this.payloadSize = payloadSize;
        this.mainScript = mainScript;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public String getMainScript() {
        return mainScript;
    }
}