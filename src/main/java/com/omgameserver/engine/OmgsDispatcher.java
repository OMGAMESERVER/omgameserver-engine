package com.omgameserver.engine;

import com.crionuke.bolts.Dispatcher;
import org.springframework.stereotype.Component;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
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
