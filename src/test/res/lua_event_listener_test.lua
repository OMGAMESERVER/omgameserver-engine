local test = {}

function test:lua_event(event)
    engine.log_info("id=" .. event.id .. ", data=" .. event.data)
    testing.lua_event_received(event.id, event.data)
end

engine.add_event_listener("lua_event", test)