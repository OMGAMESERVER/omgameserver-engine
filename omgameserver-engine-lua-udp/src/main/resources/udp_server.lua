local TOPIC = "udp"

local CLIENT_CONNECTED_EVENT_ID = "udp_client_connected";
local CLIENT_DISCONNECTED_EVENT_ID = "udp_client_disconnected";
local DATA_RECEIVED_EVENT_ID = "udp_data_received";

local SEND_EVENT_ID = "udp_send";

local function listen_udp_client_connected_on(handler)
    assert(handler, "handler is nil")
    assert(handler[CLIENT_CONNECTED_EVENT_ID],
            CLIENT_CONNECTED_EVENT_ID .. " method not found on handler")
    engine.add_event_listener(CLIENT_CONNECTED_EVENT_ID, listener)
end

local function listen_udp_client_disconnected_on(handler)
    assert(handler, "handler is nil")
    assert(handler[CLIENT_DISCONNECTED_EVENT_ID],
            CLIENT_DISCONNECTED_EVENT_ID .. " method not found on handler")
    engine.add_event_listener(CLIENT_DISCONNECTED_EVENT_ID, listener)
end

local function listen_data_received_on(handler)
    assert(handler, "handler is nil")
    assert(handler[DATA_RECEIVED_EVENT_ID],
            DATA_RECEIVED_EVENT_ID .. " method not found on handler")
    engine.add_event_listener(DATA_RECEIVED_EVENT_ID, listener)
end

local function send_data(client_uid, data, reliable)
    assert(client_uid, "parameter client_uid not specified")
    assert(data, "parameter data not specified")
    assert(reliable, "parameter reliable not specified")
    engine.dispatch("udp", "udp_send", {
        id = "udp_send", data = data, reliable = reliable
    })
end

-- Export functions
return {
    listen_udp_client_connected_on = listen_udp_client_connected_on,
    listen_udp_client_disconnected_on = listen_udp_client_disconnected_on,
    listen_data_received_on = listen_data_received_on,
    send_data = send_data,
}
