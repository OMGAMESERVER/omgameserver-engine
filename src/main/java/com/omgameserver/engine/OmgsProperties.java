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
public class OmgsProperties {

    private final InetAddress host;
    private final int port;
    private final int threadPoolSize;
    private final int queueSize;
    private final int tickInterval;
    private final int disconnectInterval;
    private final int pingInterval;
    private final int datagramSize;
    private final String mainScript;

    OmgsProperties(@Value("${omgameserver.host:0.0.0.0}") String host,
                   @Value("${omgameserver.port:12345}") int port,
                   @Value("${omgameserver.threadPoolSize:32}") int threadPoolSize,
                   @Value("${omgameserver.queueSize:128}") int queueSize,
                   @Value("${omgameserver.tickInterval:100}") int tickInterval,
                   @Value("${omgameserver.disconnectInterval:5000}") int disconnectInterval,
                   @Value("${omgameserver.pingInterval:1000}") int pingInterval,
                   @Value("${omgameserver.datagramSize:1024}") int datagramSize,
                   @Value("${omgameserver.mainScript:main.lua}") String mainScript) throws UnknownHostException {
        this.host = InetAddress.getByName(host);
        this.port = port;
        this.threadPoolSize = threadPoolSize;
        this.queueSize = queueSize;
        this.tickInterval = tickInterval;
        this.disconnectInterval = disconnectInterval;
        this.pingInterval = pingInterval;
        this.datagramSize = datagramSize;
        this.mainScript = mainScript;
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public int getQueueSize() {
        return queueSize;
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

    public int getDatagramSize() {
        return datagramSize;
    }

    public String getMainScript() {
        return mainScript;
    }
}
