local test = {}

function test:custom_event(event)
    engine.log_info("id=" .. event.id .. ", data=" .. event.data)
    testing.custom_event_received(event.id, event.data)
end

engine.add_event_listener("custom_event", test)