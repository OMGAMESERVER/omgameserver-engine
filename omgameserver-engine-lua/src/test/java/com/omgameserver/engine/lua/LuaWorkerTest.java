package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import com.omgameserver.engine.lua.events.LuaCustomEventReceivedEvent;
import com.omgameserver.engine.lua.events.LuaOutgoingValueEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaWorkerTest extends LuaBaseTest {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorkerTest.class);

    private BlockingQueue<LuaCustomEventReceivedEvent> luaCustomEventReceivedEvents;
    private BlockingQueue<LuaCustomEvent> luaCustomEvents;
    private BlockingQueue<LuaOutgoingValueEvent> luaOutgoingValueEvents;
    private ConsumerStub consumerStub;

    @BeforeEach
    void beforeEach() throws UnknownHostException {
        createComponents("stub.lua");
        luaCustomEventReceivedEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        luaCustomEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        luaOutgoingValueEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @AfterEach
    void afterEach() {
        consumerStub.finish();
    }

    @Test
    void testCustomLuaEventListener() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(coreExecutors, coreDispatcher, luaProperties, luaGlobalsFactory,
                "lua_custom_event_listener_test.lua");
        luaWorker.postConstruct();
        String eventId = "custom_event";
        String eventData = "helloworld";
        LuaValue luaEvent = new LuaTable();
        luaEvent.set("id", eventId);
        luaEvent.set("data", eventData);
        coreDispatcher.dispatch(new LuaCustomEvent(eventId, luaEvent));
        LuaCustomEventReceivedEvent luaCustomEventReceivedEvent = luaCustomEventReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        luaWorker.finish();
        assertNotNull(luaCustomEventReceivedEvent);
        assertEquals(eventId, luaCustomEventReceivedEvent.getEventId());
        assertEquals(eventData, luaCustomEventReceivedEvent.getData());
    }

    @Test
    void testDispatchFunction() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(coreExecutors, coreDispatcher, luaProperties, luaGlobalsFactory,
                "lua_dispatch_function_test.lua");
        LuaCustomEvent luaCustomEvent = luaCustomEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaCustomEvent);
        assertEquals("custom_event", luaCustomEvent.getId());
        LuaValue luaEvent = luaCustomEvent.getEvent();
        assertEquals("custom_event", luaEvent.get("id").tojstring());
        assertEquals("helloworld", luaEvent.get("data").tojstring());
    }

    @Test
    void testSendFunction() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(coreExecutors, coreDispatcher, luaProperties, luaGlobalsFactory,
                "lua_send_function_test.lua");
        LuaOutgoingValueEvent luaOutgoingValueEvent = luaOutgoingValueEvents.poll(POLL_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
        assertNotNull(luaOutgoingValueEvent);
        assertEquals(1, luaOutgoingValueEvent.getClientUid());
        assertEquals("helloworld", luaOutgoingValueEvent.getLuaValue().tojstring());
        assertEquals(true, luaOutgoingValueEvent.isReliable());
    }

    private class ConsumerStub extends Bolt implements
            LuaCustomEventReceivedEvent.Handler,
            LuaCustomEvent.Handler,
            LuaOutgoingValueEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", LUA_QUEUE_SIZE);
        }

        @Override
        public void handleLuaCustomEventReceivedEvent(LuaCustomEventReceivedEvent event) throws InterruptedException {
            luaCustomEventReceivedEvents.put(event);
        }

        @Override
        public void handleLuaCustomEvent(LuaCustomEvent event) throws InterruptedException {
            luaCustomEvents.put(event);
        }

        @Override
        public void handleLuaOutgoingValue(LuaOutgoingValueEvent event) throws InterruptedException {
            luaOutgoingValueEvents.put(event);
        }

        void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, LuaCustomEventReceivedEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, LuaCustomEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, LuaOutgoingValueEvent.class);
        }
    }
}
