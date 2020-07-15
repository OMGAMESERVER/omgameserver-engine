package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.BaseServiceTest;
import com.omgameserver.engine.events.IncomingLuaValueEvent;
import com.omgameserver.engine.events.IncomingPayloadEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class DecoderServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(DecoderServiceTest.class);

    private DecoderService decoderService;
    private BlockingQueue<IncomingLuaValueEvent> incomingLuaValueEvents;
    private ConsumerStub consumerStub;

    @Before
    public void beforeTest() throws UnknownHostException {
        createComponents();
        decoderService = new DecoderService(properties, executors, dispatcher);
        decoderService.postConstruct();
        incomingLuaValueEvents = new LinkedBlockingQueue<>(PROPERTY_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @After
    public void afterTest() {
        decoderService.finish();
        consumerStub.finish();
    }

    @Test
    public void testInteger() throws InterruptedException {
        int[] bytes = {136, 175, 112, 111, 115, 105, 116, 105, 118, 101, 95, 102, 105, 120, 105, 110, 116, 64, 165, 117,
                110, 105, 116, 56, 204, 160, 166, 117, 110, 105, 116, 49, 54, 205, 4, 0, 166, 117, 105, 110, 116, 51,
                50, 206, 0, 15, 255, 255, 175, 110, 101, 103, 97, 116, 105, 118, 101, 95, 102, 105, 120, 105, 110, 116,
                240, 164, 105, 110, 116, 56, 208, 192, 165, 105, 110, 116, 49, 54, 209, 240, 1, 165, 105, 110, 116, 51,
                50, 210, 240, 0, 0, 1};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        IncomingLuaValueEvent incomingLuaValueEvent =
                incomingLuaValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got event {}", incomingLuaValueEvent);
        // Asserts
        assertNotNull(incomingLuaValueEvent);
        assertTrue(incomingLuaValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = incomingLuaValueEvent.getLuaValue();
        assertNotNull(luaValue.get("positive_fixint"));
        assertTrue(luaValue.get("positive_fixint").checkint() == 64);
        assertNotNull(luaValue.get("unit8"));
        assertTrue(luaValue.get("unit8").checkint() == 160);
        assertNotNull(luaValue.get("unit16"));
        assertTrue(luaValue.get("unit16").checkint() == 1024);
        assertNotNull(luaValue.get("uint32"));
        assertTrue(luaValue.get("uint32").checkint() == 1048575);
        assertNotNull(luaValue.get("negative_fixint"));
        assertTrue(luaValue.get("negative_fixint").checkint() == -16);
        assertNotNull(luaValue.get("int8"));
        assertTrue(luaValue.get("int8").checkint() == -64);
        assertNotNull(luaValue.get("int16"));
        assertTrue(luaValue.get("int16").checkint() == -4095);
        assertNotNull(luaValue.get("int32"));
        assertTrue(luaValue.get("int32").checkint() == -268435455);
    }

    @Test
    public void testBigInteger() throws InterruptedException {
        int[] bytes = {130, 166, 117, 105, 110, 116, 54, 52, 207, 66, 6, 254, 224, 225, 168, 0, 0, 165, 105, 110, 116,
                54, 52, 211, 66, 54, 254, 224, 229, 45, 0, 0};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        IncomingLuaValueEvent incomingLuaValueEvent =
                incomingLuaValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got event {}", incomingLuaValueEvent);
        // Asserts
        assertNotNull(incomingLuaValueEvent);
        assertTrue(incomingLuaValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = incomingLuaValueEvent.getLuaValue();
        assertNotNull(luaValue.get("uint64"));
        // uint64 not supported, in client and server replaced by double
        assertTrue(Math.round(luaValue.get("uint64").checkdouble()) == 12345678901L);
        assertNotNull(luaValue.get("int64"));
        // int64 not supported, in client and server replaced by double
        assertTrue(Math.round(luaValue.get("int64").checkdouble()) == 98765432109L);
    }

    @Test
    public void testBoolean() throws InterruptedException {
        int[] bytes = {130, 164, 116, 114, 117, 101, 195, 165, 102, 97, 108, 115, 101, 194};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        IncomingLuaValueEvent incomingLuaValueEvent =
                incomingLuaValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got event {}", incomingLuaValueEvent);
        // Asserts
        assertNotNull(incomingLuaValueEvent);
        assertTrue(incomingLuaValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = incomingLuaValueEvent.getLuaValue();
        assertNotNull(luaValue.get("true"));
        assertTrue(luaValue.get("true").checkboolean());
        assertNotNull(luaValue.get("false"));
        assertTrue(!luaValue.get("false").checkboolean());
    }

    @Test
    public void testStrings() throws InterruptedException {
        int[] bytes = {132, 166, 102, 105, 120, 115, 116, 114, 166, 70, 105, 120, 115, 116, 114, 164, 115, 116, 114, 56,
                217, 40, 83, 116, 114, 56, 115, 116, 114, 56, 115, 116, 114, 56, 115, 116, 114, 56, 115, 116, 114, 56,
                115, 116, 114, 56, 115, 116, 114, 56, 115, 116, 114, 56, 115, 116, 114, 56, 115, 116, 114, 56, 165, 115,
                116, 114, 49, 54, 218, 1, 9, 83, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115,
                116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114,
                49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115,
                116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114,
                49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115,
                116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114,
                49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115,
                116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114,
                49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115,
                116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114,
                49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115,
                116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114, 49, 54, 115, 116, 114,
                49, 54, 168, 208, 186, 208, 187, 209, 142, 209, 135, 176, 208, 183, 208, 189, 208, 176, 209, 135, 208,
                181, 208, 189, 208, 184, 208, 181};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        IncomingLuaValueEvent incomingLuaValueEvent =
                incomingLuaValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got event {}", incomingLuaValueEvent);
        // Asserts
        assertNotNull(incomingLuaValueEvent);
        assertTrue(incomingLuaValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = incomingLuaValueEvent.getLuaValue();
        assertNotNull(luaValue.get("fixstr"));
        assertTrue(luaValue.get("fixstr").checkjstring().equals("Fixstr"));
        assertNotNull(luaValue.get("str8"));
        assertTrue(luaValue.get("str8").checkjstring().equals("Str8str8str8str8str8str8str8str8str8str8"));
        assertNotNull(luaValue.get("str16"));
        assertTrue(luaValue.get("str16").checkjstring().equals("Str16str16str16str16str16str16str16str16str16str16str" +
                "16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str" +
                "16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str" +
                "16str16str16"));
        assertNotNull(luaValue.get("ключ"));
        assertTrue(luaValue.get("ключ").checkjstring().equals("значение"));
    }

    @Test
    public void testArray() throws InterruptedException {
        int[] bytes = {130, 168, 102, 105, 120, 97, 114, 114, 97, 121, 153, 1, 2, 3, 4, 5, 6, 7, 8, 9, 167, 97, 114,
                114, 97, 121, 49, 54, 220, 0, 32, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        IncomingLuaValueEvent incomingLuaValueEvent =
                incomingLuaValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got event {}", incomingLuaValueEvent);
        // Asserts
        assertNotNull(incomingLuaValueEvent);
        assertTrue(incomingLuaValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = incomingLuaValueEvent.getLuaValue();
        LuaValue fixArray = luaValue.get("fixarray");
        assertNotNull(fixArray);
        for (int i = 1; i <= 9; i++) {
            assertTrue(fixArray.get(i).checkint() == i);
        }
        LuaValue array16 = luaValue.get("array16");
        assertNotNull(array16);
        for (int i = 1; i <= 32; i++) {
            assertTrue(array16.get(i).checkint() == i);
        }
    }

    @Test
    public void testMap() throws InterruptedException {
        int[] bytes = {130, 166, 102, 105, 120, 109, 97, 112, 132, 161, 49, 1, 161, 50, 2, 161, 52, 4, 161, 56, 8, 165,
                109, 97, 112, 49, 54, 222, 0, 17, 161, 49, 1, 161, 50, 2, 161, 52, 4, 161, 56, 8, 162, 49, 54, 16, 162,
                51, 50, 32, 162, 54, 52, 64, 163, 49, 50, 56, 204, 128, 163, 50, 53, 54, 205, 1, 0, 163, 53, 49, 50,
                205, 2, 0, 164, 49, 48, 50, 52, 205, 4, 0, 164, 50, 48, 52, 56, 205, 8, 0, 164, 52, 48, 57, 54, 205,
                16, 0, 164, 56, 49, 57, 50, 205, 32, 0, 165, 49, 54, 51, 56, 52, 205, 64, 0, 165, 51, 50, 55, 54, 56,
                205, 128, 0, 165, 54, 53, 53, 51, 54, 206, 0, 1, 0, 0};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        dispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        IncomingLuaValueEvent incomingLuaValueEvent =
                incomingLuaValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        logger.info("Got event {}", incomingLuaValueEvent);
        // Asserts
        assertNotNull(incomingLuaValueEvent);
        assertTrue(incomingLuaValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = incomingLuaValueEvent.getLuaValue();
        LuaValue fixMap = luaValue.get("fixmap");
        assertNotNull(fixMap);
        for (int i = 1; i <= 8; i *= 2) {
            assertTrue(fixMap.get(String.valueOf(i)).checkint() == i);
        }
        LuaValue map16 = luaValue.get("map16");
        assertNotNull(map16);
        for (int i = 1; i <= 65536 + 1; i *= 2) {
            assertTrue(map16.get(String.valueOf(i)).checkint() == i);
        }
    }

    private IncomingPayloadEvent createIncomingPayloadEvent(SocketAddress socketAddress, long clientUid, int[] bytes) {
        ByteBuffer payload = ByteBuffer.allocate(PROPERTY_DATAGRAM_SIZE);
        for (int i = 0; i < bytes.length; i++) {
            payload.put((byte) (bytes[i] & 0xFF));
        }
        payload.flip();
        return new IncomingPayloadEvent(socketAddress, clientUid, payload);
    }

    private class ConsumerStub extends Bolt implements
            IncomingLuaValueEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", PROPERTY_QUEUE_SIZE);
        }

        @Override
        public void handleIncomingLuaValue(IncomingLuaValueEvent event) throws InterruptedException {
            incomingLuaValueEvents.put(event);
        }

        void postConstruct() {
            executors.executeInInternalPool(this);
            dispatcher.getDispatcher().subscribe(this, IncomingLuaValueEvent.class);
        }
    }
}
