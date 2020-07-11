package com.omgameserver.engine;

import com.crionuke.bolts.Dispatcher;
import org.springframework.stereotype.Component;

@Component
public class OmgsDispatcher {

    private final Dispatcher dispatcher;

    OmgsDispatcher() {
        dispatcher = new Dispatcher();
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }
}
