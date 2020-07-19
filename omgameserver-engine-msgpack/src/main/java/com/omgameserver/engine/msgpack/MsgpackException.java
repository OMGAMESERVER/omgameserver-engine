package com.omgameserver.engine.msgpack;

public class MsgpackException extends Exception {

    public MsgpackException(String message) {
        super(message);
    }

    public MsgpackException(String message, Throwable cause) {
        super(message, cause);
    }
}
