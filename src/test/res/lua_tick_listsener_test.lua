local test = {}

function test:tick(event)
    runtime.log_info("tick id=" .. event.id .. ", tick_number=" .. event.tick_number .. ", delta_time=" .. event.delta_time)
    testing.tick_received(event.tick_number, event.delta_time)
end

runtime.add_event_listener("tick", test)
