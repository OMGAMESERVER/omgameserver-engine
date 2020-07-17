package com.omgameserver.engine.udp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
class UdpProperties {

    private final int queueSize;
    private final InetAddress host;
    private final int port;
    private final int datagramSize;
    private final int disconnectInterval;
    private final int pingInterval;

    UdpProperties(@Value("${omgameserver.engine.udp.queueSize:128}") int queueSize,
                  @Value("${omgameserver.engine.udp.host:0.0.0.0}") String host,
                  @Value("${omgameserver.engine.udp.port:12345}") int port,
                  @Value("${omgameserver.engine.udp.datagramSize:1024}") int datagramSize,
                  @Value("${omgameserver.engine.udp.disconnectInterval:5000}") int disconnectInterval,
                  @Value("${omgameserver.engine.udp.pingInterval:500}") int pingInterval) throws UnknownHostException {
        this.queueSize = queueSize;
        this.host = InetAddress.getByName(host);
        this.port = port;
        this.datagramSize = datagramSize;
        this.disconnectInterval = disconnectInterval;
        this.pingInterval = pingInterval;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getDatagramSize() {
        return datagramSize;
    }

    public int getDisconnectInterval() {
        return disconnectInterval;
    }

    public int getPingInterval() {
        return pingInterval;
    }
}