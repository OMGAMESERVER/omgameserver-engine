local test = {}

function test:client_connected(event)
    runtime.log_info("id=" .. event.id .. ", client_uid=" .. event.client_uid)
    testing.client_connected_received(event.client_uid, event.data)
end

runtime.add_event_listener("client_connected", test)