local test = {}

function test:custom_event(event)
    runtime.log_info("id=" .. event.id .. ", data=" .. event.data)
    testing.custom_event_received(event.id, event.data)
end

runtime.add_event_listener("custom_event", test)