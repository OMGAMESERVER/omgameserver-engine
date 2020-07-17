package com.omgameserver.engine.lua;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.lua.events.LuaOutgoingValueEvent;
import com.omgameserver.engine.udp.events.UdpOutgoingPayloadEvent;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
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
class LuaMsgpackEncoderService extends Bolt implements
        LuaOutgoingValueEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaMsgpackEncoderService.class);

    // Use & 0xFF for unsigned byte in int datatype
    private final int MSG_PACK_TRUE = 0xc3 & 0xFF;
    private final int MSG_PACK_FALSE = 0xc2 & 0xFF;
    private final int MSG_PACK_UINT8 = 0xcc & 0xFF;
    private final int MSG_PACK_UINT16 = 0xcd & 0xFF;
    private final int MSG_PACK_UINT32 = 0xce & 0xFF;
    private final int MSG_PACK_UINT64 = 0xcf & 0xFF;
    private final int MSG_PACK_INT8 = 0xd0 & 0xFF;
    private final int MSG_PACK_INT16 = 0xd1 & 0xFF;
    private final int MSG_PACK_INT32 = 0xd2 & 0xFF;
    private final int MSG_PACK_INT64 = 0xd3 & 0xFF;
    private final int MSG_PACK_FLOAT32 = 0xca & 0xFF;
    private final int MSG_PACK_FLOAT64 = 0xcb & 0xFF;
    private final int MSG_PACK_STR8 = 0xd9 & 0xFF;
    private final int MSG_PACK_STR16 = 0xda & 0xFF;
    private final int MSG_PACK_STR32 = 0xdb & 0xFF;
    private final int MSG_PACK_ARRAY16 = 0xdc & 0xFF;
    private final int MSG_PACK_ARRAY32 = 0xdd & 0xFF;
    private final int MSG_PACK_MAP32 = 0xdf & 0xFF;
    private final int MSG_PACK_MAP16 = 0xde & 0xFF;

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final LuaProperties properties;

    LuaMsgpackEncoderService(CoreExecutors executors, CoreDispatcher dispatcher, LuaProperties properties) {
        super("lua-msgpack-encoder", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.properties = properties;
    }

    @Override
    public void handleLuaOutgoingValue(LuaOutgoingValueEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        LuaValue luaValue = event.getLuaValue();
        boolean reliable = event.isReliable();
        ByteBuffer payload = null;
        try {
            payload = ByteBuffer.allocate(properties.getPayloadSize());
            // Encode LuaValue to MsgPack
            encode(payload, luaValue);
            payload.flip();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Encoding luavalue for clientUid={} failed with {}", event.getClientUid(), e);
            }
            payload = null;
        }
        if (payload != null) {
            dispatcher.dispatch(new UdpOutgoingPayloadEvent(clientUid, payload, reliable));
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, LuaOutgoingValueEvent.class);
    }

    private ByteBuffer encode(ByteBuffer rawData, LuaValue luaValue) {
        return encodeValue(luaValue, rawData);
    }

    private ByteBuffer encodeValue(LuaValue luaValue, ByteBuffer rawData) {
        long length;

        switch (luaValue.type()) {
            case LuaValue.TBOOLEAN:
                if (luaValue.checkboolean()) {
                    return rawData.put((byte) MSG_PACK_TRUE);
                } else {
                    return rawData.put((byte) MSG_PACK_FALSE);
                }

            case LuaValue.TNUMBER:
                if (luaValue.isint()) {
                    long value = luaValue.checklong();
                    if (value >= 0) {
                        if (value < 128) {
                            return rawData.put((byte) value);
                        } else if (value < 256) {
                            return rawData.put((byte) MSG_PACK_UINT8).put((byte) (value & 0xFF));
                        } else if (value < 65536) {
                            return rawData.put((byte) MSG_PACK_UINT16).putShort((short) (value & 0xFFFF));
                        } else if (value < 4294967296L) {
                            return rawData.put((byte) MSG_PACK_UINT32).putInt((int) (value & 0xFFFFFFFFL));
                        } else {
                            // uint 64 - not supported, in client and server replaced by double
                            return rawData.put((byte) MSG_PACK_UINT64).putDouble(value);
                        }

                    } else {
                        if (value >= -32) {
                            return rawData.put((byte) (0xe0 + (value + 32)));
                        } else if (value >= -128) {
                            return rawData.put((byte) MSG_PACK_INT8).put((byte) value);
                        } else if (value >= -32768) {
                            return rawData.put((byte) MSG_PACK_INT16).putShort((short) value);
                        } else if (value >= -2147483648) {
                            return rawData.put((byte) MSG_PACK_INT32).putInt((int) value);
                        } else {
                            // int 64 - not supported, in client and server replaced by double
                            return rawData.put((byte) MSG_PACK_INT64).putDouble(value);
                        }
                    }

                } else {
                    double value = luaValue.checkdouble();
                    if (value == (float) value) {
                        return rawData.put((byte) MSG_PACK_FLOAT32).putFloat((float) value);
                    } else {
                        return rawData.put((byte) MSG_PACK_FLOAT64).putDouble(value);
                    }
                }

            case LuaValue.TSTRING:
                LuaString value = luaValue.checkstring();
                length = value.length();

                if (length < 32) {
                    return rawData.put((byte) ((0xa0 + length) & 0xFF))
                            .put(value.m_bytes);
                } else if (length < 256) {
                    return rawData.put((byte) MSG_PACK_STR8).put((byte) (length & 0xFF))
                            .put(value.m_bytes);
                } else if (length < 65536) {
                    return rawData.put((byte) MSG_PACK_STR16).putShort((short) (length & 0xFFFF))
                            .put(value.m_bytes);
                } else {
                    return rawData.put((byte) MSG_PACK_STR32).putInt((int) (length & 0xFFFFFFFFL))
                            .put(value.m_bytes);
                }

            case LuaValue.TTABLE:
                LuaTable table = luaValue.checktable();
                length = size(table);

                // Array
                if (isArray(table)) {
                    if (length < 16) {
                        rawData.put((byte) ((0x90 + length) & 0xFF));
                    } else if (length < 65536) {
                        rawData.put((byte) MSG_PACK_ARRAY16).putShort((short) (length & 0xFFFF));
                    } else {
                        rawData.put((byte) MSG_PACK_ARRAY32).putInt((int) (length & 0xFFFFFFFFL));
                    }
                    LuaValue k = LuaValue.NIL;
                    while (true) {
                        Varargs item = luaValue.next(k);
                        if ((k = item.arg1()).isnil()) {
                            break;
                        }
                        LuaValue v = item.arg(2);
                        encodeValue(v, rawData);
                    }

                    // Map
                } else {
                    if (length < 16) {
                        rawData.put((byte) ((0x80 + length) & 0xFF));
                    } else if (length < 65536) {
                        rawData.put((byte) MSG_PACK_MAP16).putShort((short) (length & 0xFFFF));
                    } else {
                        rawData.put((byte) MSG_PACK_MAP32).putInt((int) (length & 0xFFFFFFFFL));
                    }
                    LuaValue k = LuaValue.NIL;
                    while (true) {
                        Varargs item = luaValue.next(k);
                        if ((k = item.arg1()).isnil()) {
                            break;
                        }
                        LuaValue v = item.arg(2);
                        encodeValue(k, rawData);
                        encodeValue(v, rawData);
                    }
                }

                return rawData;
        }

        return null;
    }

    private boolean isArray(LuaValue luaValue) {
        int expected = 1;
        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs item = luaValue.next(k);
            k = item.arg1();
            if (k.isnil()) {
                break;
            }
            if (k.isint() && k.checkint() == expected) {
                expected = expected + 1;
            } else {
                return false;
            }
        }
        return true;
    }

    private int size(LuaTable luaTable) {
        int result = 0;
        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs item = luaTable.next(k);
            k = item.arg1();
            if (k.isnil()) {
                break;
            }
            result = result + 1;
        }
        return result;
    }
}
