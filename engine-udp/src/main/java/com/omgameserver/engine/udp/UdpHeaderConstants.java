package com.omgameserver.engine.udp;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
interface UdpHeaderConstants {
    // UdpHeaderConstants size = seq + ack + bit + sys
    int HEADER_SIZE = 3 * Integer.BYTES + Byte.BYTES;
    // No value header
    byte HEADER_SYS_NOVALUE = 1;
    // Ping request
    byte HEADER_SYS_PINGREQ = 2;
    // Pong response
    byte HEADER_SYS_PONGRES = 4;
}
