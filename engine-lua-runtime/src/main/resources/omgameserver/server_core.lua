local CORE_TICK_EVENT_ID = "core_tick";

local function listen_core_tick_event_on(handler)
    assert(handler, "handler is nil")
    assert(handler[CORE_TICK_EVENT_ID],
            CORE_TICK_EVENT_ID .. " method not found on handler")
    runtime.add_event_listener(CORE_TICK_EVENT_ID, handler)
end

-- Export functions
return {
    listen_core_tick_event_on = listen_core_tick_event_on,
}