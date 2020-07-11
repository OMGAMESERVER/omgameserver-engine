package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
import com.omgameserver.engine.events.LuaDataReceivedEvent;
import com.omgameserver.engine.events.LuaTickReceivedEvent;
import com.omgameserver.engine.events.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LuaWorkerTest extends LuaBaseTest {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorkerTest.class);

    private LuaWorker luaWorker;
    private BlockingQueue<LuaDataReceivedEvent> luaDataReceivedEvents;
    private BlockingQueue<LuaTickReceivedEvent> luaTickReceivedEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_worker_test.lua");
        luaWorker.postConstruct();
        luaDataReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        luaTickReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        luaWorker.finish();
        consumerStub.finish();
    }

    @Test
    public void testIncomingLuaValueEvent() throws InterruptedException {
        long clientUid = 1;
        String data = "helloworld";
        LuaValue luaValue = LuaString.valueOf(data);
        dispatcher.getDispatcher().dispatch(new IncomingLuaValueEvent(clientUid, luaValue), luaWorker);
        LuaDataReceivedEvent luaDataReceivedEvent = luaDataReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaDataReceivedEvent);
        assertEquals(clientUid, luaDataReceivedEvent.getClientUid());
        assertEquals(data, luaDataReceivedEvent.getData());
    }

    @Test
    public void testTickEvent() throws InterruptedException {
        long tickNumber = 1;
        long tickDeltaTime = 100;
        dispatcher.getDispatcher().dispatch(new TickEvent(tickNumber, tickDeltaTime));
        LuaTickReceivedEvent luaTickReceivedEvent = luaTickReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaTickReceivedEvent);
        assertEquals(tickNumber, luaTickReceivedEvent.getNumber());
        assertEquals(tickDeltaTime, luaTickReceivedEvent.getDeltaTime());
    }

    private class ConsumerStub extends Bolt implements
            LuaDataReceivedEvent.Handler,
            LuaTickReceivedEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleLuaDataReceived(LuaDataReceivedEvent event) throws InterruptedException {
            luaDataReceivedEvents.put(event);
        }

        @Override
        public void handleLuaTickReceivedEvent(LuaTickReceivedEvent event) throws InterruptedException {
            luaTickReceivedEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, LuaDataReceivedEvent.class);
            dispatcher.getDispatcher().subscribe(this, LuaTickReceivedEvent.class);
        }
    }
}
