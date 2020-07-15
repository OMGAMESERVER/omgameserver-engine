package com.omgameserver.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
public class EngineProperties {

    private final InetAddress host;
    private final int port;
    private final int internalThreadPoolSize;
    private final int userThreadPoolSize;
    private final int queueSize;
    private final int datagramSize;
    private final int tickInterval;
    private final int disconnectInterval;
    private final int pingInterval;
    private final String mainScript;

    EngineProperties(@Value("${omgameserver.engine.host:0.0.0.0}") String host,
                     @Value("${omgameserver.engine.port:12345}") int port,
                     @Value("${omgameserver.engine.internalThreadPoolSize:16}") int internalThreadPoolSize,
                     @Value("${omgameserver.engine.userThreadPoolSize:32}") int userThreadPoolSize,
                     @Value("${omgameserver.engine.queueSize:128}") int queueSize,
                     @Value("${omgameserver.engine.datagramSize:1024}") int datagramSize,
                     @Value("${omgameserver.engine.tickInterval:100}") int tickInterval,
                     @Value("${omgameserver.engine.disconnectInterval:5000}") int disconnectInterval,
                     @Value("${omgameserver.engine.pingInterval:500}") int pingInterval,
                     @Value("${omgameserver.engine.mainScript:main.lua}") String mainScript) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        this.port = port;
        this.internalThreadPoolSize = internalThreadPoolSize;
        this.userThreadPoolSize = userThreadPoolSize;
        this.queueSize = queueSize;
        this.datagramSize = datagramSize;
        this.tickInterval = tickInterval;
        this.disconnectInterval = disconnectInterval;
        this.pingInterval = pingInterval;
        this.mainScript = mainScript;
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getInternalThreadPoolSize() {
        return internalThreadPoolSize;
    }

    public int getUserThreadPoolSize() {
        return userThreadPoolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getDatagramSize() {
        return datagramSize;
    }

    public int getTickInterval() {
        return tickInterval;
    }

    public int getDisconnectInterval() {
        return disconnectInterval;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public String getMainScript() {
        return mainScript;
    }
}
