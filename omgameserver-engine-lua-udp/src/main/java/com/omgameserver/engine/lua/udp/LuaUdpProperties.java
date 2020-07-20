package com.omgameserver.engine.lua.udp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
class LuaUdpProperties {

    private final int queueSize;
    private final int payloadSize;

    LuaUdpProperties(@Value("${omgameserver.engine.lua.queueSize:128}") int queueSize,
                     @Value("${omgameserver.engine.lua.payloadSize:1024}") int payloadSize) {
        this.queueSize = queueSize;
        this.payloadSize = payloadSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getPayloadSize() {
        return payloadSize;
    }
}
