local test = {}

function test:tick(event)
    engine.log_info("id=" .. event.id .. ", tick_number=" .. event.tick_number .. ", delta_time=" .. event.delta_time)
    testing.tick_received(event.tick_number, event.delta_time)
end

engine.add_event_listener("tick", test)
