package com.omgameserver.engine.luaudp;

import com.crionuke.bolts.Bolt;
import com.omgameserver.engine.core.CoreDispatcher;
import com.omgameserver.engine.core.CoreExecutors;
import com.omgameserver.engine.luaudp.events.LuaUdpIncomingValueEvent;
import com.omgameserver.engine.msgpack.MsgpackDecoder;
import com.omgameserver.engine.msgpack.MsgpackException;
import com.omgameserver.engine.udp.events.UdpIncomingPayloadEvent;
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
class LuaUdpDecoderService extends Bolt implements
        UdpIncomingPayloadEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(LuaUdpDecoderService.class);

    private final CoreExecutors executors;
    private final CoreDispatcher dispatcher;
    private final MsgpackDecoder msgpackDecoder;

    LuaUdpDecoderService(CoreExecutors executors, CoreDispatcher dispatcher, MsgpackDecoder msgpackDecoder,
                         LuaUdpProperties properties) {
        super("lua-udp-decoder", properties.getQueueSize());
        this.executors = executors;
        this.dispatcher = dispatcher;
        this.msgpackDecoder = msgpackDecoder;
    }

    @Override
    public void handleUdpIncomingPayload(UdpIncomingPayloadEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        ByteBuffer payload = event.getPayload();
        // Loop as payload can contain more one luatables sequentially
        while (payload.hasRemaining()) {
            LuaValue luaValue = null;
            try {
                luaValue = msgpackDecoder.decode(payload);
            } catch (MsgpackException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Decoding from {} with clientUid={} failed with {}",
                            event.getSocketAddress(), event.getClientUid(), e);
                }
            }
            if (luaValue != null) {
                dispatcher.dispatch(new LuaUdpIncomingValueEvent(clientUid, luaValue));
            }
        }
    }

    @PostConstruct
    void postConstruct() {
        executors.executeInInternalPool(this);
        dispatcher.getDispatcher().subscribe(this, UdpIncomingPayloadEvent.class);
    }
}
