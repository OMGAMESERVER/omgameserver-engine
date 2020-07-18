package com.omgameserver.engine.luaudp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.luaudp.events.LuaUdpIncomingValueEvent;
import com.omgameserver.engine.udp.events.UdpIncomingPayloadEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class LuaUdpDecoderServiceTest extends BaseServiceTest {
    static private final Logger logger = LoggerFactory.getLogger(LuaUdpDecoderServiceTest.class);

    private LuaUdpDecoderService decoderService;
    private BlockingQueue<LuaUdpIncomingValueEvent> luaUdpIncomingValueEvents;
    private ConsumerStub consumerStub;

    @BeforeEach
    public void beforeEach() {
        createComponents();
        decoderService = new LuaUdpDecoderService(coreExecutors, coreDispatcher, luaUdpProperties);
        decoderService.postConstruct();
        luaUdpIncomingValueEvents = new LinkedBlockingQueue<>(LUA_UDP_QUEUE_SIZE);
        consumerStub = new ConsumerStub();
        consumerStub.postConstruct();
    }

    @AfterEach
    public void afterEach() {
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
        coreDispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        LuaUdpIncomingValueEvent luaUdpIncomingValueEvent =
                luaUdpIncomingValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Asserts
        Assertions.assertNotNull(luaUdpIncomingValueEvent);
        Assertions.assertTrue(luaUdpIncomingValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = luaUdpIncomingValueEvent.getLuaValue();
        Assertions.assertNotNull(luaValue.get("positive_fixint"));
        Assertions.assertTrue(luaValue.get("positive_fixint").checkint() == 64);
        Assertions.assertNotNull(luaValue.get("unit8"));
        Assertions.assertTrue(luaValue.get("unit8").checkint() == 160);
        Assertions.assertNotNull(luaValue.get("unit16"));
        Assertions.assertTrue(luaValue.get("unit16").checkint() == 1024);
        Assertions.assertNotNull(luaValue.get("uint32"));
        Assertions.assertTrue(luaValue.get("uint32").checkint() == 1048575);
        Assertions.assertNotNull(luaValue.get("negative_fixint"));
        Assertions.assertTrue(luaValue.get("negative_fixint").checkint() == -16);
        Assertions.assertNotNull(luaValue.get("int8"));
        Assertions.assertTrue(luaValue.get("int8").checkint() == -64);
        Assertions.assertNotNull(luaValue.get("int16"));
        Assertions.assertTrue(luaValue.get("int16").checkint() == -4095);
        Assertions.assertNotNull(luaValue.get("int32"));
        Assertions.assertTrue(luaValue.get("int32").checkint() == -268435455);
    }

    @Test
    public void testBigInteger() throws InterruptedException {
        int[] bytes = {130, 166, 117, 105, 110, 116, 54, 52, 207, 66, 6, 254, 224, 225, 168, 0, 0, 165, 105, 110, 116,
                54, 52, 211, 66, 54, 254, 224, 229, 45, 0, 0};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        coreDispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        LuaUdpIncomingValueEvent luaUdpIncomingValueEvent =
                luaUdpIncomingValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Asserts
        Assertions.assertNotNull(luaUdpIncomingValueEvent);
        Assertions.assertTrue(luaUdpIncomingValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = luaUdpIncomingValueEvent.getLuaValue();
        Assertions.assertNotNull(luaValue.get("uint64"));
        // uint64 not supported, in client and server replaced by double
        Assertions.assertTrue(Math.round(luaValue.get("uint64").checkdouble()) == 12345678901L);
        Assertions.assertNotNull(luaValue.get("int64"));
        // int64 not supported, in client and server replaced by double
        Assertions.assertTrue(Math.round(luaValue.get("int64").checkdouble()) == 98765432109L);
    }

    @Test
    public void testBoolean() throws InterruptedException {
        int[] bytes = {130, 164, 116, 114, 117, 101, 195, 165, 102, 97, 108, 115, 101, 194};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        coreDispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        LuaUdpIncomingValueEvent luaUdpIncomingValueEvent =
                luaUdpIncomingValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Asserts
        Assertions.assertNotNull(luaUdpIncomingValueEvent);
        Assertions.assertTrue(luaUdpIncomingValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = luaUdpIncomingValueEvent.getLuaValue();
        Assertions.assertNotNull(luaValue.get("true"));
        Assertions.assertTrue(luaValue.get("true").checkboolean());
        Assertions.assertNotNull(luaValue.get("false"));
        Assertions.assertTrue(!luaValue.get("false").checkboolean());
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
        coreDispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        LuaUdpIncomingValueEvent luaUdpIncomingValueEvent =
                luaUdpIncomingValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Asserts
        Assertions.assertNotNull(luaUdpIncomingValueEvent);
        Assertions.assertTrue(luaUdpIncomingValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = luaUdpIncomingValueEvent.getLuaValue();
        Assertions.assertNotNull(luaValue.get("fixstr"));
        Assertions.assertTrue(luaValue.get("fixstr").checkjstring().equals("Fixstr"));
        Assertions.assertNotNull(luaValue.get("str8"));
        Assertions.assertTrue(luaValue.get("str8").checkjstring().equals("Str8str8str8str8str8str8str8str8str8str8"));
        Assertions.assertNotNull(luaValue.get("str16"));
        Assertions.assertTrue(luaValue.get("str16").checkjstring().equals("Str16str16str16str16str16str16str16str16str16str16str" +
                "16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str" +
                "16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str16str" +
                "16str16str16"));
        Assertions.assertNotNull(luaValue.get("ключ"));
        Assertions.assertTrue(luaValue.get("ключ").checkjstring().equals("значение"));
    }

    @Test
    public void testArray() throws InterruptedException {
        int[] bytes = {130, 168, 102, 105, 120, 97, 114, 114, 97, 121, 153, 1, 2, 3, 4, 5, 6, 7, 8, 9, 167, 97, 114,
                114, 97, 121, 49, 54, 220, 0, 32, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        SocketAddress socketAddress = generateSocketAddress();
        long clientUid = generateClientUid();
        coreDispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        LuaUdpIncomingValueEvent luaUdpIncomingValueEvent =
                luaUdpIncomingValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Asserts
        Assertions.assertNotNull(luaUdpIncomingValueEvent);
        Assertions.assertTrue(luaUdpIncomingValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = luaUdpIncomingValueEvent.getLuaValue();
        LuaValue fixArray = luaValue.get("fixarray");
        Assertions.assertNotNull(fixArray);
        for (int i = 1; i <= 9; i++) {
            Assertions.assertTrue(fixArray.get(i).checkint() == i);
        }
        LuaValue array16 = luaValue.get("array16");
        Assertions.assertNotNull(array16);
        for (int i = 1; i <= 32; i++) {
            Assertions.assertTrue(array16.get(i).checkint() == i);
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
        coreDispatcher.dispatch(createIncomingPayloadEvent(socketAddress, clientUid, bytes));
        LuaUdpIncomingValueEvent luaUdpIncomingValueEvent =
                luaUdpIncomingValueEvents.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        // Asserts
        Assertions.assertNotNull(luaUdpIncomingValueEvent);
        Assertions.assertTrue(luaUdpIncomingValueEvent.getClientUid() == clientUid);
        LuaValue luaValue = luaUdpIncomingValueEvent.getLuaValue();
        LuaValue fixMap = luaValue.get("fixmap");
        Assertions.assertNotNull(fixMap);
        for (int i = 1; i <= 8; i *= 2) {
            Assertions.assertTrue(fixMap.get(String.valueOf(i)).checkint() == i);
        }
        LuaValue map16 = luaValue.get("map16");
        Assertions.assertNotNull(map16);
        for (int i = 1; i <= 65536 + 1; i *= 2) {
            Assertions.assertTrue(map16.get(String.valueOf(i)).checkint() == i);
        }
    }

    private UdpIncomingPayloadEvent createIncomingPayloadEvent(SocketAddress socketAddress, long clientUid,
                                                               int[] bytes) {
        ByteBuffer payload = ByteBuffer.allocate(luaUdpProperties.getPayloadSize());
        for (int i = 0; i < bytes.length; i++) {
            payload.put((byte) (bytes[i] & 0xFF));
        }
        payload.flip();
        return new UdpIncomingPayloadEvent(socketAddress, clientUid, payload);
    }

    private class ConsumerStub extends Bolt implements
            LuaUdpIncomingValueEvent.Handler {

        ConsumerStub() {
            super("consumer-stub", LUA_UDP_QUEUE_SIZE);
        }

        @Override
        public void handleLuaUdpIncomingValue(LuaUdpIncomingValueEvent event) throws InterruptedException {
            luaUdpIncomingValueEvents.put(event);
        }

        void postConstruct() {
            coreExecutors.executeInInternalPool(this);
            coreDispatcher.getDispatcher().subscribe(this, LuaUdpIncomingValueEvent.class);
        }
    }
}
