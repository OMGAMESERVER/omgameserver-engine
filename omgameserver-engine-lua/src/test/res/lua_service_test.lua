local test = {}

function test:tick(event)
    engine.log_info("id=" .. event.id .. ", tick_number=" .. event.tick_number .. ", delta_time=" .. event.delta_time)
    testing.tick_received(event.tick_number, event.delta_time)
end

function test:connected(event)
    engine.log_info("id=" .. event.id .. ", client_uid=" .. event.client_uid .. ", type=" .. event.client_type)
    testing.client_connected_received(event.client_uid, event.client_type)
end

function test:disconnected(event)
    engine.log_info("id=" .. event.id .. ", client_uid=" .. event.client_uid .. ", type=" .. event.client_type)
    testing.client_disconnected_received(event.client_uid, event.client_type)
end

function test:received(event)
    engine.log_info("id=" .. event.id .. ", client_uid=" .. event.client_uid .. ", data=" .. event.data)
    testing.data_received(event.client_uid, event.data)
end

engine.add_event_listener("tick", test)
engine.add_event_listener("connected", test)
engine.add_event_listener("disconnected", test)
engine.add_event_listener("received", test)