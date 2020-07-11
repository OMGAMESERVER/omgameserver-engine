local test = {}

function test:received(event)
    runtime.log_info("received id=" .. event.id .. ", client_uid=" .. event.client_uid .. ", data=" .. event.data)
    testing.data_received(event.client_uid, event.data)
end

function test:tick(event)
    runtime.log_info("tick id=" .. event.id .. ", tick_number=" .. event.tick_number .. ", delta_time=" .. event.delta_time)
    testing.tick_received(event.tick_number, event.delta_time)
end

runtime.add_event_listener("received", test)
runtime.add_event_listener("tick", test)
