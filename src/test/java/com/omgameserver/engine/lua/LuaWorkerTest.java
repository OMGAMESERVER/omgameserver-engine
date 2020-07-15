package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.events.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class LuaWorkerTest extends LuaBaseTest {
    static private final Logger logger = LoggerFactory.getLogger(LuaWorkerTest.class);

    private BlockingQueue<LuaClientConnectedReceivedEvent> luaClientConnectedReceivedEvents;
    private BlockingQueue<LuaClientDisconnectedReceivedEvent> luaClientDisconnectedReceivedEvents;
    private BlockingQueue<LuaDataReceivedEvent> luaDataReceivedEvents;
    private BlockingQueue<LuaTickReceivedEvent> luaTickReceivedEvents;
    private BlockingQueue<OutgoingLuaValueEvent> outgoingLuaValueEvents;
    private BlockingQueue<DisconnectClientRequestEvent> disconnectClientRequestEvents;
    private BlockingQueue<LuaEvent> luaEvents;
    private BlockingQueue<LuaEventReceivedEvent> luaEventReceivedEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        luaClientConnectedReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        luaClientDisconnectedReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        luaDataReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        luaTickReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        outgoingLuaValueEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        disconnectClientRequestEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        luaEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        luaEventReceivedEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        consumerStub.finish();
    }

    @Test
    public void testClientConnectedListener() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_client_connected_listener_test.lua");
        luaWorker.postConstruct();
        long clientUid = 1;
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.dispatch(new ClientConnectedEvent(socketAddress, clientUid));
        LuaClientConnectedReceivedEvent luaClientConnectedReceivedEvent =
                luaClientConnectedReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        luaWorker.finish();
        assertNotNull(luaClientConnectedReceivedEvent);
        assertEquals(clientUid, luaClientConnectedReceivedEvent.getClientUid());
    }

    @Test
    public void testClientDisconnectedListener() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_client_disconnected_listener_test.lua");
        luaWorker.postConstruct();
        long clientUid = 1;
        SocketAddress socketAddress = generateSocketAddress();
        dispatcher.dispatch(new ClientDisconnectedEvent(socketAddress, clientUid));
        LuaClientDisconnectedReceivedEvent luaClientDisconnectedReceivedEvent =
                luaClientDisconnectedReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        luaWorker.finish();
        assertNotNull(luaClientDisconnectedReceivedEvent);
        assertEquals(clientUid, luaClientDisconnectedReceivedEvent.getClientUid());
    }

    @Test
    public void testReceivedListener() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_received_listsener_test.lua");
        luaWorker.postConstruct();
        long clientUid = 1;
        String data = "helloworld";
        LuaValue luaValue = LuaString.valueOf(data);
        dispatcher.dispatch(new IncomingLuaValueEvent(clientUid, luaValue), luaWorker);
        LuaDataReceivedEvent luaDataReceivedEvent = luaDataReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        luaWorker.finish();
        assertNotNull(luaDataReceivedEvent);
        assertEquals(clientUid, luaDataReceivedEvent.getClientUid());
        assertEquals(data, luaDataReceivedEvent.getData());
    }

    @Test
    public void testTickListener() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_tick_listsener_test.lua");
        luaWorker.postConstruct();
        long tickNumber = 1;
        long tickDeltaTime = 100;
        dispatcher.dispatch(new TickEvent(tickNumber, tickDeltaTime));
        LuaTickReceivedEvent luaTickReceivedEvent = luaTickReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        luaWorker.finish();
        assertNotNull(luaTickReceivedEvent);
        assertEquals(tickNumber, luaTickReceivedEvent.getNumber());
        assertEquals(tickDeltaTime, luaTickReceivedEvent.getDeltaTime());
    }

    @Test
    public void testDispatchFunction() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_dispatch_function_test.lua");
        LuaEvent luaEvent = luaEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaEvent);
        assertEquals("lua_event", luaEvent.getId());
        LuaValue event = luaEvent.getEvent();
        assertEquals("lua_event", event.get("id").tojstring());
        assertEquals("helloworld", event.get("data").tojstring());
    }

    @Test
    public void testSendFunction() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_send_function_test.lua");
        OutgoingLuaValueEvent outgoingLuaValueEvent = outgoingLuaValueEvents.poll(POLL_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
        assertNotNull(outgoingLuaValueEvent);
        assertEquals(1, outgoingLuaValueEvent.getClientUid());
        assertEquals("helloworld", outgoingLuaValueEvent.getLuaValue().tojstring());
        assertEquals(true, outgoingLuaValueEvent.isReliable());
    }

    @Test
    public void testDiconnectFunction() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_disconnect_function_test.lua");
        DisconnectClientRequestEvent disconnectClientRequestEvent = disconnectClientRequestEvents.poll(POLL_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
        assertNotNull(disconnectClientRequestEvent);
        assertEquals(1, disconnectClientRequestEvent.getClientUid());
    }

    @Test
    public void testLuaEventListener() throws InterruptedException {
        LuaWorker luaWorker = new LuaWorker(properties, executors, dispatcher, luaGlobals,
                "lua_event_listener_test.lua");
        luaWorker.postConstruct();
        String eventId = "lua_event";
        String eventData = "helloworld";
        LuaValue luaEvent = new LuaTable();
        luaEvent.set("id", eventId);
        luaEvent.set("data", eventData);
        dispatcher.dispatch(new LuaEvent(eventId, luaEvent));
        LuaEventReceivedEvent luaEventReceivedEvent = luaEventReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        luaWorker.finish();
        assertNotNull(luaEventReceivedEvent);
        assertEquals(eventId, luaEventReceivedEvent.getEventId());
        assertEquals(eventData, luaEventReceivedEvent.getData());
    }

    private class ConsumerStub extends Bolt implements
            LuaClientConnectedReceivedEvent.Handler,
            LuaClientDisconnectedReceivedEvent.Handler,
            LuaDataReceivedEvent.Handler,
            LuaTickReceivedEvent.Handler,
            OutgoingLuaValueEvent.Handler,
            DisconnectClientRequestEvent.Handler,
            LuaEvent.Handler,
            LuaEventReceivedEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleLuaClientConnectedReceivedEvent(LuaClientConnectedReceivedEvent event) throws InterruptedException {
            luaClientConnectedReceivedEvents.put(event);
        }

        @Override
        public void handleLuaClientDisconnectedReceivedEvent(LuaClientDisconnectedReceivedEvent event) throws InterruptedException {
            luaClientDisconnectedReceivedEvents.put(event);
        }

        @Override
        public void handleLuaDataReceived(LuaDataReceivedEvent event) throws InterruptedException {
            luaDataReceivedEvents.put(event);
        }

        @Override
        public void handleLuaTickReceivedEvent(LuaTickReceivedEvent event) throws InterruptedException {
            luaTickReceivedEvents.put(event);
        }

        @Override
        public void handleOutgoingLuaValue(OutgoingLuaValueEvent event) throws InterruptedException {
            outgoingLuaValueEvents.put(event);
        }

        @Override
        public void handleDisconnectClientRequest(DisconnectClientRequestEvent event) throws InterruptedException {
            disconnectClientRequestEvents.put(event);
        }

        @Override
        public void handleLuaEvent(LuaEvent event) throws InterruptedException {
            luaEvents.put(event);
        }

        @Override
        public void handleLuaEventReceivedEvent(LuaEventReceivedEvent event) throws InterruptedException {
            luaEventReceivedEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, LuaClientConnectedReceivedEvent.class);
            dispatcher.getDispatcher().subscribe(this, LuaClientDisconnectedReceivedEvent.class);
            dispatcher.getDispatcher().subscribe(this, LuaDataReceivedEvent.class);
            dispatcher.getDispatcher().subscribe(this, LuaTickReceivedEvent.class);
            dispatcher.getDispatcher().subscribe(this, OutgoingLuaValueEvent.class);
            dispatcher.getDispatcher().subscribe(this, DisconnectClientRequestEvent.class);
            dispatcher.getDispatcher().subscribe(this, LuaEvent.class);
            dispatcher.getDispatcher().subscribe(this, LuaEventReceivedEvent.class);
        }
    }
}
