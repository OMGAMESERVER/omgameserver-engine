package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.events.CoreTickEvent;
import com.omgameserver.engine.lua.events.*;
import com.omgameserver.engine.udp.events.UdpClientConnectedEvent;
import com.omgameserver.engine.udp.events.UdpClientDisconnectedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class LuaServiceTest extends LuaBaseTest implements LuaEventConstants {
    static private final Logger logger = LoggerFactory.getLogger(LuaServiceTest.class);

    private LuaService luaService;
    private BlockingQueue<LuaTickReceivedEvent> luaTickReceivedEvents;
    private BlockingQueue<LuaConnectedReceivedEvent> luaConnectedReceivedEvents;
    private BlockingQueue<LuaDisconnectedReceivedEvent> luaDisconnectedReceivedEvents;
    private BlockingQueue<LuaDataReceivedEvent> luaDataReceivedEvents;
    private ConsumerStub consumerStub;

    @BeforeEach
    void beforeEach() throws UnknownHostException {
        createComponents("lua_service_test.lua");
        luaService = new LuaService(coreExecutors, coreDispatcher, luaProperties, luaGlobalsFactory);
        luaService.postConstruct();
        luaTickReceivedEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        luaConnectedReceivedEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        luaDisconnectedReceivedEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
        luaDataReceivedEvents = new LinkedBlockingQueue<>(LUA_QUEUE_SIZE);
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

    @Test
    void testUdpClientConnected() throws InterruptedException {
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        coreDispatcher.dispatch(new UdpClientConnectedEvent(socketAddress, clientUid));
        LuaConnectedReceivedEvent luaConnectedReceivedEvent =
                luaConnectedReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaConnectedReceivedEvent);
        assertEquals(clientUid, luaConnectedReceivedEvent.getClientUid());
        assertEquals(UDP_CLIENT_TYPE, luaConnectedReceivedEvent.getClientType());
    }

    @Test
    void testUdpClientDisconnected() throws InterruptedException {
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        coreDispatcher.dispatch(new UdpClientDisconnectedEvent(socketAddress, clientUid));
        LuaDisconnectedReceivedEvent luaDisconnectedReceivedEvent =
                luaDisconnectedReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaDisconnectedReceivedEvent);
        assertEquals(clientUid, luaDisconnectedReceivedEvent.getClientUid());
        assertEquals(UDP_CLIENT_TYPE, luaDisconnectedReceivedEvent.getClientType());
    }

    @Test
    void testIncomingValue() throws InterruptedException {
        long clientUid = generateClientUid();
        String data = "helloworld";
        coreDispatcher.dispatch(new LuaIncomingValueEvent(clientUid, LuaString.valueOf(data)));
        LuaDataReceivedEvent luaDataReceivedEvent = luaDataReceivedEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(luaDataReceivedEvent);
        assertEquals(clientUid, luaDataReceivedEvent.getClientUid());
        assertEquals(data, luaDataReceivedEvent.getData());
    }

    private class ConsumerStub extends Bolt implements
            LuaTickReceivedEvent.Handler,
            LuaConnectedReceivedEvent.Handler,
            LuaDisconnectedReceivedEvent.Handler,
            LuaDataReceivedEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", LUA_QUEUE_SIZE);
        }

        @Override
        public void handleLuaTickReceivedEvent(LuaTickReceivedEvent event) throws InterruptedException {
            luaTickReceivedEvents.put(event);
        }

        @Override
        public void handleLuaConnectedReceivedEvent(LuaConnectedReceivedEvent event) throws InterruptedException {
            luaConnectedReceivedEvents.put(event);
        }

        @Override
        public void handleLuaDisconnectedReceivedEvent(LuaDisconnectedReceivedEvent event) throws InterruptedException {
            luaDisconnectedReceivedEvents.put(event);
        }

        @Override
        public void handleLuaDataReceived(LuaDataReceivedEvent event) throws InterruptedException {
            luaDataReceivedEvents.put(event);
        }

        void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, LuaTickReceivedEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, LuaConnectedReceivedEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, LuaDisconnectedReceivedEvent.class);
            coreDispatcher.getDispatcher().subscribe(this, LuaDataReceivedEvent.class);
        }
    }
}
