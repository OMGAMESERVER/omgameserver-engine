package com.omgameserver.engine.lua.msgpack;

public class MsgpackException extends Exception {

    public MsgpackException(String message) {
        super(message);
    }

    public MsgpackException(String message, Throwable cause) {
        super(message, cause);
    }
}
