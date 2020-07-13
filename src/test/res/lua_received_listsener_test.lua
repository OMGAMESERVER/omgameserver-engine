local test = {}

function test:received(event)
    runtime.log_info("id=" .. event.id .. ", client_uid=" .. event.client_uid .. ", data=" .. event.data)
    testing.data_received(event.client_uid, event.data)
end

runtime.add_event_listener("received", test)