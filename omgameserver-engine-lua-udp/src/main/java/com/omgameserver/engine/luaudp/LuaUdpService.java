package com.omgameserver.engine.luaudp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.lua.events.LuaCustomEvent;
import com.omgameserver.engine.lua.events.LuaDirectEvent;
import com.omgameserver.engine.luaudp.events.LuaUdpIncomingValueEvent;
import com.omgameserver.engine.luaudp.events.LuaUdpOutgoingValueEvent;
import com.omgameserver.engine.udp.events.UdpClientConnectedEvent;
import com.omgameserver.engine.udp.events.UdpClientDisconnectedEvent;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.omgameserver.engine.luaudp.LuaUdpEvents.*;
import static com.omgameserver.engine.luaudp.LuaUdpTopics.LUA_UDP_TOPIC;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class LuaUdpService extends Bolt implements
        UdpClientConnectedEvent.Handler,
        UdpClientDisconnectedEvent.Handler,
        LuaUdpIncomingValueEvent.Handler,
        LuaCustomEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaUdpService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;

    LuaUdpService(CoreExecutors executors, CoreDispatcher dispatcher, LuaUdpProperties properties) {
        super("lua-udp", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleUdpClientConnected(UdpClientConnectedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", LUA_UDP_CLIENT_CONNECTED_EVENT_ID);
        luaEvent.set("client_uid", event.getClientUid());
        dispatcher.dispatch(new LuaCustomEvent(LUA_UDP_CLIENT_CONNECTED_EVENT_ID, luaEvent));
    }

    @Override
    public void handleUdpClientDisconnected(UdpClientDisconnectedEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", LUA_UDP_CLIENT_DISCONNECTED_EVENT_ID);
        luaEvent.set("client_uid", event.getClientUid());
        dispatcher.dispatch(new LuaCustomEvent(LUA_UDP_CLIENT_DISCONNECTED_EVENT_ID, luaEvent));
    }

    @Override
    public void handleLuaUdpIncomingValue(LuaUdpIncomingValueEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        LuaValue data = event.getLuaValue();
        LuaTable luaEvent = new LuaTable();
        luaEvent.set("id", LUA_UDP_DATA_RECEIVED_EVENT_ID);
        luaEvent.set("client_uid", clientUid);
        luaEvent.set("data", data);
        dispatcher.dispatch(new LuaDirectEvent(clientUid,
                new LuaCustomEvent(LUA_UDP_DATA_RECEIVED_EVENT_ID, luaEvent)));
    }

    @Override
    public void handleLuaCustomEvent(LuaCustomEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        String eventId = event.getEventId();
        switch (eventId) {
            case LUA_UDP_SEND_EVENT_ID:
                if (!handleUdpSendEvent(event) && logger.isWarnEnabled()) {
                    logger.warn("Hanling of {} failed, check passed arguments", eventId);
                }
                break;
            default:
                if (logger.isWarnEnabled()) {
                    logger.warn("{} got unrecognized event {}", this, event);
                }
                return;
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, UdpClientConnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, UdpClientDisconnectedEvent.class);
        dispatcher.getDispatcher().subscribe(this, LuaUdpIncomingValueEvent.class);
        dispatcher.getDispatcher().subscribe(this, LUA_UDP_TOPIC);
    }

    private boolean handleUdpSendEvent(LuaCustomEvent event) throws InterruptedException {
        LuaValue luaEvent = event.getLuaEvent();
        LuaValue luaClientUid = luaEvent.get("client_uid");
        LuaValue luaData = luaEvent.get("data");
        LuaValue luaReliable = luaEvent.get("reliable");
        if (luaClientUid == LuaValue.NIL || luaData == LuaValue.NIL || luaReliable == LuaValue.NIL) {
            return false;
        } else {
            try {
                dispatcher.dispatch(new LuaUdpOutgoingValueEvent(luaClientUid.checklong(), luaData,
                        luaReliable.checkboolean()));
                return true;
            } catch (LuaError e) {
                return false;
            }
        }
    }
}
