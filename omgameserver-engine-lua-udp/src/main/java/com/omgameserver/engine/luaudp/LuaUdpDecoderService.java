package com.omgameserver.engine.luaudp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.luaudp.events.LuaUdpIncomingValueEvent;
import com.omgameserver.engine.udp.events.UdpIncomingPayloadEvent;
import org.luaj.vm2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class LuaUdpDecoderService extends Bolt implements
        UdpIncomingPayloadEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaUdpDecoderService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;

    LuaUdpDecoderService(CoreExecutors executors, CoreDispatcher dispatcher, LuaUdpProperties properties) {
        super("lua-udp-decoder", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleUdpIncomingPayload(UdpIncomingPayloadEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        ByteBuffer payload = event.getPayload();
        // Loop as payload can contain more one luatables sequentially
        while (payload.hasRemaining()) {
            LuaValue luaValue = null;
            try {
                luaValue = decode(payload);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Decoding payload from {} failed with {}", event.getClientUid(), e);
                }
            }
            if (luaValue != null) {
                dispatcher.dispatch(new LuaUdpIncomingValueEvent(clientUid, luaValue));
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, UdpIncomingPayloadEvent.class);
    }

    private LuaValue decode(ByteBuffer byteBuffer) {
        return decodeValue(byteBuffer);
    }

    private LuaValue decodeValue(ByteBuffer byteBuffer) {
        // Get unsigned byte
        int id = (byteBuffer.get() & 0xff);
        int length;

        switch (id) {
            // false
            case 0xc2:
                return LuaBoolean.FALSE;
            // true
            case 0xc3:
                return LuaBoolean.TRUE;
            // float32
            case 0xca:
                return LuaDouble.valueOf(byteBuffer.getFloat());
            // float64
            case 0xcb:
                return LuaDouble.valueOf(byteBuffer.getDouble());
            // uint8
            case 0xcc:
                return LuaNumber.valueOf(byteBuffer.get() & 0xff);
            // uint16
            case 0xcd:
                return LuaNumber.valueOf(byteBuffer.getShort() & 0xffff);
            // uint32
            case 0xce:
                return LuaNumber.valueOf(byteBuffer.getInt() & 0xffffffffL);
            // uint64 - not supported, in client and server replaced by double
            case 0xcf:
                return LuaDouble.valueOf(byteBuffer.getDouble());
            // int8
            case 0xd0:
                return LuaNumber.valueOf(byteBuffer.get());
            // int16
            case 0xd1:
                return LuaNumber.valueOf(byteBuffer.getShort());
            // int32
            case 0xd2:
                return LuaNumber.valueOf(byteBuffer.getInt());
            // int64 - not supported, in client and server replaced by double
            case 0xd3:
                return LuaNumber.valueOf(byteBuffer.getDouble());
            // str8
            case 0xd9:
                length = (byteBuffer.get() & 0xff);
                byteBuffer.position(byteBuffer.position() + length);
                return LuaString.valueOf(byteBuffer.array(), byteBuffer.position() - length, length);
            // str16
            case 0xda:
                length = (byteBuffer.getShort() & 0xffff);
                byteBuffer.position(byteBuffer.position() + length);
                return LuaString.valueOf(byteBuffer.array(), byteBuffer.position() - length, length);
            // array16
            case 0xdc:
                length = (byteBuffer.getShort() & 0xffff);
                return decodeArray(byteBuffer, length);
            // map16
            case 0xde:
                length = (byteBuffer.getShort() & 0xffff);
                return decodeMap(byteBuffer, length);
        }

        // fixint
        if (id >= 0x00 && id <= 0x7f) {
            return LuaNumber.valueOf(id);
        }

        // fixmap
        if (id >= 0x80 && id <= 0x8f) {
            return decodeMap(byteBuffer, id - 0x80);
        }

        // fixarray
        if (id >= 0x90 && id <= 0x9f) {
            return decodeArray(byteBuffer, id - 0x90);
        }

        // fixstr
        if (id >= 0xa0 && id <= 0xbf) {
            length = id - 0xa0;
            byteBuffer.position(byteBuffer.position() + length);
            return LuaString.valueOf(byteBuffer.array(), byteBuffer.position() - length, length);
        }

        // negative fixint
        if (id >= 0xe0 && id <= 0xff) {
            return LuaNumber.valueOf(-32 + (id - 0xe0));
        }

        throw new UnsupportedOperationException("Unsupported msgpack format 0x" + String.format("%x", id));
    }

    private LuaTable decodeArray(ByteBuffer byteBuffer, int length) {
        LuaValue[] array = new LuaValue[length];
        for (int i = 0; i < length; i++) {
            array[i] = decodeValue(byteBuffer);
        }
        return LuaTable.listOf(array);
    }

    private LuaTable decodeMap(ByteBuffer byteBuffer, int length) {
        LuaValue[] array = new LuaValue[length * 2];
        for (int i = 0; i < length * 2; i++) {
            array[i] = decodeValue(byteBuffer);
        }
        return LuaTable.tableOf(array);
    }
}