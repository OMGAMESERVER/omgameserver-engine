package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.events.LuaTickEventReceived;
import com.omgameserver.engine.events.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LuaWorkerTest extends LuaBaseTest {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorkerTest.class);

    private LuaWorker luaWorker;
    private BlockingQueue<LuaTickEventReceived> luaTickEventReceiveds;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_worker_test.lua");
        luaWorker.postConstruct();
        luaTickEventReceiveds = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        luaWorker.finish();
        consumerStub.finish();
    }

    @Test
    public void testTickEvent() throws InterruptedException {
        long tickNumber = 1;
        long tickDeltaTime = 100;
        dispatcher.dispatch(new TickEvent(tickNumber, tickDeltaTime));
        LuaTickEventReceived luaTickEventReceived = luaTickEventReceiveds.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaTickEventReceived);
        assertEquals(tickNumber, luaTickEventReceived.getNumber());
        assertEquals(tickDeltaTime, luaTickEventReceived.getDeltaTime());
    }

    private class ConsumerStub extends Bolt implements
            LuaTickEventReceived.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleLuaTickEventReceived(LuaTickEventReceived event) throws InterruptedException {
            luaTickEventReceiveds.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.subscribe(this, LuaTickEventReceived.class);
        }
    }
}
