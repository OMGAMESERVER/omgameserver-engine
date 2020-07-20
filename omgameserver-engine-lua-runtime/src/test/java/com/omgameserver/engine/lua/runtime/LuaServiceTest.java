package com.omgameserver.engine.lua.runtime;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.lua.runtime.events.LuaTickReceivedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class LuaServiceTest extends LuaBaseTest implements LuaEvents {
    static private final Logger logger = LoggerFactory.getLogger(LuaServiceTest.class);

    private LuaService luaService;
    private BlockingQueue<LuaTickReceivedEvent> luaTickReceivedEvents;
    private ConsumerStub consumerStub;

    @BeforeEach
    void beforeEach() throws UnknownHostException {
        createComponents("lua_service_test.lua");
        luaService = new LuaService(coreExecutors, coreDispatcher, luaProperties, luaGlobalsFactory);
        luaService.postConstruct();
        luaTickReceivedEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @AfterEach
    void afterEach() {
        consumerStub.finish();
        luaService.finish();
    }

    @Test
    void testTick() throws InterruptedException {
        long tickNumber = 1;
        long tickDeltaTime = 100;
        coreDispatcher.dispatch(new CoreTickEvent(tickNumber, tickDeltaTime));
        LuaTickReceivedEvent luaTickReceivedEvent = luaTickReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaTickReceivedEvent);
        assertEquals(tickNumber, luaTickReceivedEvent.getNumber());
        assertEquals(tickDeltaTime, luaTickReceivedEvent.getDeltaTime());
    }

    private class ConsumerStub extends Bolt implements
            LuaTickReceivedEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", LUA_QUEUE_SIZE);
        }

        @Override
        public void handleLuaTickReceivedEvent(LuaTickReceivedEvent event) throws InterruptedException {
            luaTickReceivedEvents.put(event);
        }

        void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, LuaTickReceivedEvent.class);
        }
    }
}
