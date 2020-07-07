package com.omgameserver.engine.transformation;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.OutgoingLuaValueEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
public class EncodingService extends Bolt implements
        OutgoingLuaValueEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(EncodingService.class);

    private final OmgsProperties properties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;

    EncodingService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("encoder", properties.getQueueSize());
        this.properties = properties;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleOutgoingLuaValue(OutgoingLuaValueEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        LuaValue luaValue = event.getLuaValue();
        boolean ephemeral = event.isEphemeral();
        // TODO: encoding luavalue to bytes
        ByteBuffer rawData = ByteBuffer.allocate(100);
        dispatcher.dispatch(new OutgoingRawDataEvent(clientUid, rawData, ephemeral));
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, OutgoingLuaValueEvent.class);
    }
}
