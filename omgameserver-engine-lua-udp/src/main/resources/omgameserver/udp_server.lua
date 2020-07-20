local MODULE_TOPIC = "udp"

local UDP_CLIENT_CONNECTED_EVENT_ID = "udp_client_connected";
local UDP_CLIENT_DISCONNECTED_EVENT_ID = "udp_client_disconnected";
local UDP_DATA_RECEIVED_EVENT_ID = "udp_data_received";
local UDP_SEND_EVENT_ID = "udp_send_data";

local function listen_udp_client_connected_event_on(handler)
    assert(handler, "handler is nil")
    assert(handler[UDP_CLIENT_CONNECTED_EVENT_ID],
            UDP_CLIENT_CONNECTED_EVENT_ID .. " method not found on handler")
    runtime.add_event_listener(UDP_CLIENT_CONNECTED_EVENT_ID, handler)
end

local function listen_udp_client_disconnected_event_on(handler)
    assert(handler, "handler is nil")
    assert(handler[UDP_CLIENT_DISCONNECTED_EVENT_ID],
            UDP_CLIENT_DISCONNECTED_EVENT_ID .. " method not found on handler")
    runtime.add_event_listener(UDP_CLIENT_DISCONNECTED_EVENT_ID, handler)
end

local function listen_udp_data_received_event_on(handler)
    assert(handler, "handler is nil")
    assert(handler[UDP_DATA_RECEIVED_EVENT_ID],
            UDP_DATA_RECEIVED_EVENT_ID .. " method not found on handler")
    runtime.add_event_listener(UDP_DATA_RECEIVED_EVENT_ID, handler)
end

local function dispatch_udp_send_data_event(client_uid, data, reliable)
    assert(client_uid, "parameter client_uid not specified")
    assert(data, "parameter data not specified")
    assert(reliable, "parameter reliable not specified")
    runtime.dispatch(MODULE_TOPIC, UDP_SEND_EVENT_ID, {
        id = UDP_SEND_EVENT_ID, data = data, reliable = reliable
    })
end

-- Export functions
return {
    listen_udp_client_connected_event_on = listen_udp_client_connected_event_on,
    listen_udp_client_disconnected_event_on = listen_udp_client_disconnected_event_on,
    listen_udp_data_received_event_on = listen_udp_data_received_event_on,
    dispatch_udp_send_data_event = dispatch_udp_send_data_event,
}
