local test = {}

function test:client_connected(event)
    engine.log_info("id=" .. event.id .. ", client_uid=" .. event.client_uid)
    testing.client_connected_received(event.client_uid)
end

engine.add_event_listener("client_connected", test)