package com.omgameserver.engine;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public interface OmgsConstants {
    // Header size = seq + ack + bit + sys
    int HEADER_SIZE = 3 * Integer.BYTES + Byte.BYTES;
    // No value header
    byte HEADER_SYS_NOVALUE = 0x00;
    // Ping request from client
    byte HEADER_SYS_PINGREQ = 0x01;
    // Pong response from client
    byte HEADER_SYS_PONGRES = 0x02;
}
