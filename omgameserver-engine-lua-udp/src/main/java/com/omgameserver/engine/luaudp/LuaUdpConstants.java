package com.omgameserver.engine.luaudp;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
interface LuaUdpConstants {
    String LUA_UDP_CLIENT_CONNECTED_EVENT_ID = "udp_client_connected";
    String LUA_UDP_CLIENT_DISCONNECTED_EVENT_ID = "udp_client_disconnected";
    String LUA_UDP_DATA_RECEIVED_EVENT_ID = "udp_data_received";
    String LUA_UDP_SEND_EVENT_ID = "udp_send";
}
