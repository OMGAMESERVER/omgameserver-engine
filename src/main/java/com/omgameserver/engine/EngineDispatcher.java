package com.omgameserver.engine;

import com.crionuke.bolts.Dispatcher;
import com.crionuke.bolts.Event;
import org.springframework.stereotype.Component;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
public class EngineDispatcher {

    private final Dispatcher dispatcher;

    EngineDispatcher() {
        dispatcher = new Dispatcher();
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public boolean dispatch(Event event) throws InterruptedException {
        return dispatcher.dispatch(event);
    }

    public boolean dispatch(Event event, Object topic) throws InterruptedException {
        return dispatcher.dispatch(event, topic);
    }
}
