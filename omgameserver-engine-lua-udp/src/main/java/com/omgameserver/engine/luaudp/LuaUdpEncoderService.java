package com.omgameserver.engine.luaudp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.luaudp.events.LuaUdpOutgoingValueEvent;
import com.omgameserver.engine.msgpack.MsgpackEncoder;
import com.omgameserver.engine.msgpack.MsgpackException;
import com.omgameserver.engine.udp.events.UdpOutgoingPayloadEvent;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class LuaUdpEncoderService extends Bolt implements
        LuaUdpOutgoingValueEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaUdpEncoderService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final MsgpackEncoder msgpackEncoder;
    private final LuaUdpProperties properties;

    LuaUdpEncoderService(CoreExecutors executors, CoreDispatcher dispatcher, MsgpackEncoder msgpackEncoder,
                         LuaUdpProperties properties) {
        super("lua-udp-encoder", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.msgpackEncoder = msgpackEncoder;
        this.properties = properties;
    }

    @Override
    public void handleLuaUdpOutgoingValue(LuaUdpOutgoingValueEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        LuaValue luaValue = event.getLuaValue();
        boolean reliable = event.isReliable();
        ByteBuffer payload = null;
        try {
            payload = ByteBuffer.allocate(properties.getPayloadSize());
            // Encode luavalue to msgpack
            msgpackEncoder.encode(luaValue, payload);
            payload.flip();
        } catch (MsgpackException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Encoding for clientUid={} failed with {}", event.getClientUid(), e);
            }
            payload = null;
        }
        if (payload != null) {
            dispatcher.dispatch(new UdpOutgoingPayloadEvent(clientUid, payload, reliable));
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, LuaUdpOutgoingValueEvent.class);
    }
}
