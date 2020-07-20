package com.omgameserver.engine.lua.msgpack;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

@Component
public class MsgpackEncoder {
    static private final Logger logger = LoggerFactory.getLogger(MsgpackEncoder.class);

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

    public ByteBuffer encode(LuaValue luaValue, ByteBuffer byteBuffer) throws MsgpackException {
        if (luaValue == null) {
            throw new NullPointerException("luaValue is null");
        }
        if (byteBuffer == null) {
            throw new NullPointerException("byteBuffer is null");
        }
        try {
            return encodeValue(luaValue, byteBuffer);
        } catch (BufferOverflowException e) {
            throw new MsgpackException("Buffer size too small", e);
        }
    }

    private ByteBuffer encodeValue(LuaValue luaValue, ByteBuffer byteBuffer) {
        long length;

        switch (luaValue.type()) {
            case LuaValue.TBOOLEAN:
                if (luaValue.checkboolean()) {
                    return byteBuffer.put((byte) MSG_PACK_TRUE);
                } else {
                    return byteBuffer.put((byte) MSG_PACK_FALSE);
                }

            case LuaValue.TNUMBER:
                if (luaValue.isint()) {
                    long value = luaValue.checklong();
                    if (value >= 0) {
                        if (value < 128) {
                            return byteBuffer.put((byte) value);
                        } else if (value < 256) {
                            return byteBuffer.put((byte) MSG_PACK_UINT8).put((byte) (value & 0xFF));
                        } else if (value < 65536) {
                            return byteBuffer.put((byte) MSG_PACK_UINT16).putShort((short) (value & 0xFFFF));
                        } else if (value < 4294967296L) {
                            return byteBuffer.put((byte) MSG_PACK_UINT32).putInt((int) (value & 0xFFFFFFFFL));
                        } else {
                            // uint 64 - not supported, in client and server replaced by double
                            return byteBuffer.put((byte) MSG_PACK_UINT64).putDouble(value);
                        }

                    } else {
                        if (value >= -32) {
                            return byteBuffer.put((byte) (0xe0 + (value + 32)));
                        } else if (value >= -128) {
                            return byteBuffer.put((byte) MSG_PACK_INT8).put((byte) value);
                        } else if (value >= -32768) {
                            return byteBuffer.put((byte) MSG_PACK_INT16).putShort((short) value);
                        } else if (value >= -2147483648) {
                            return byteBuffer.put((byte) MSG_PACK_INT32).putInt((int) value);
                        } else {
                            // int 64 - not supported, in client and server replaced by double
                            return byteBuffer.put((byte) MSG_PACK_INT64).putDouble(value);
                        }
                    }

                } else {
                    double value = luaValue.checkdouble();
                    if (value == (float) value) {
                        return byteBuffer.put((byte) MSG_PACK_FLOAT32).putFloat((float) value);
                    } else {
                        return byteBuffer.put((byte) MSG_PACK_FLOAT64).putDouble(value);
                    }
                }

            case LuaValue.TSTRING:
                LuaString value = luaValue.checkstring();
                length = value.length();

                if (length < 32) {
                    return byteBuffer.put((byte) ((0xa0 + length) & 0xFF))
                            .put(value.m_bytes);
                } else if (length < 256) {
                    return byteBuffer.put((byte) MSG_PACK_STR8).put((byte) (length & 0xFF))
                            .put(value.m_bytes);
                } else if (length < 65536) {
                    return byteBuffer.put((byte) MSG_PACK_STR16).putShort((short) (length & 0xFFFF))
                            .put(value.m_bytes);
                } else {
                    return byteBuffer.put((byte) MSG_PACK_STR32).putInt((int) (length & 0xFFFFFFFFL))
                            .put(value.m_bytes);
                }

            case LuaValue.TTABLE:
                LuaTable table = luaValue.checktable();
                length = size(table);

                // Array
                if (isArray(table)) {
                    if (length < 16) {
                        byteBuffer.put((byte) ((0x90 + length) & 0xFF));
                    } else if (length < 65536) {
                        byteBuffer.put((byte) MSG_PACK_ARRAY16).putShort((short) (length & 0xFFFF));
                    } else {
                        byteBuffer.put((byte) MSG_PACK_ARRAY32).putInt((int) (length & 0xFFFFFFFFL));
                    }
                    LuaValue k = LuaValue.NIL;
                    while (true) {
                        Varargs item = luaValue.next(k);
                        if ((k = item.arg1()).isnil()) {
                            break;
                        }
                        LuaValue v = item.arg(2);
                        encodeValue(v, byteBuffer);
                    }

                    // Map
                } else {
                    if (length < 16) {
                        byteBuffer.put((byte) ((0x80 + length) & 0xFF));
                    } else if (length < 65536) {
                        byteBuffer.put((byte) MSG_PACK_MAP16).putShort((short) (length & 0xFFFF));
                    } else {
                        byteBuffer.put((byte) MSG_PACK_MAP32).putInt((int) (length & 0xFFFFFFFFL));
                    }
                    LuaValue k = LuaValue.NIL;
                    while (true) {
                        Varargs item = luaValue.next(k);
                        if ((k = item.arg1()).isnil()) {
                            break;
                        }
                        LuaValue v = item.arg(2);
                        encodeValue(k, byteBuffer);
                        encodeValue(v, byteBuffer);
                    }
                }

                return byteBuffer;
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
