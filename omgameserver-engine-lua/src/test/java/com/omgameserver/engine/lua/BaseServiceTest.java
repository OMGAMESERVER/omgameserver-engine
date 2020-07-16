package com.omgameserver.engine.lua;

import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.core.CoreProperties;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class BaseServiceTest extends Assertions {
    static private final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected final int POLL_TIMEOUT_MS = 1000;

    protected final int CORE_INTERNAL_THREAD_POOL_SIZE = 16;
    protected final int CORE_USER_THREAD_POOL_SIZE = 32;
    protected final int CORE_TICK_INTERVAL = 100;

    protected final int LUA_QUEUE_SIZE = 32;
    protected final int LUA_PAYLOAD_SIZE = 1024;

    protected CoreProperties coreProperties;
    protected CoreDispatcher coreDispatcher;
    protected CoreExecutors coreExecutors;
    protected LuaProperties luaProperties;

    protected void createComponents(String mainScript) throws UnknownHostException {
        coreProperties = new CoreProperties(CORE_INTERNAL_THREAD_POOL_SIZE, CORE_USER_THREAD_POOL_SIZE,
                CORE_TICK_INTERVAL);
        coreDispatcher = new CoreDispatcher();
        coreExecutors = new CoreExecutors(coreProperties);
        luaProperties = new LuaProperties(LUA_QUEUE_SIZE, LUA_PAYLOAD_SIZE, mainScript);
    }

    protected SocketAddress generateSocketAddress() {
        SocketAddress socketAddress = new InetSocketAddress("0.0.0.0", (int) (Math.random() * 55535) + 10000);
        logger.info("New socketAddress={} generated", socketAddress);
        return socketAddress;
    }

    protected Long generateClientUid() {
        Long clientUid = Long.valueOf((int) (Math.random() * 89999 + 10000));
        logger.info("New clientUid={} generated", clientUid);
        return clientUid;
    }
}
