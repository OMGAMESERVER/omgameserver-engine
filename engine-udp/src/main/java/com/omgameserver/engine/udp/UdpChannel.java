package com.omgameserver.engine.udp;

import com.omgameserver.engine.udp.events.UdpIncomingDatagramEvent;
import com.omgameserver.engine.udp.events.UdpOutgoingDatagramEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Component
class UdpChannel implements UdpHeaderConstants {
    static private final Logger logger = LoggerFactory.getLogger(UdpChannel.class);

    private final UdpProperties properties;
    private final DatagramChannel datagramChannel;
    private final Receiver receiver;
    private final Sender sender;

    UdpChannel(UdpProperties properties) throws IOException {
        this.properties = properties;
        datagramChannel = DatagramChannel.open();
        datagramChannel.bind(new InetSocketAddress(properties.getHost(), properties.getPort()));
        logger.info("Datagram channel binded to {}", datagramChannel.getLocalAddress());
        receiver = new Receiver(datagramChannel);
        sender = new Sender(datagramChannel);
    }

    Receiver getReceiver() {
        return receiver;
    }

    Sender getSender() {
        return sender;
    }

    SocketAddress getAddress() throws IOException {
        return datagramChannel.getLocalAddress();
    }

    int getPort() throws IOException {
        return ((InetSocketAddress) datagramChannel.getLocalAddress()).getPort();
    }

    void close() throws IOException {
        datagramChannel.close();
    }

    class Receiver {
        private final DatagramChannel datagramChannel;

        Receiver(DatagramChannel datagramChannel) {
            this.datagramChannel = datagramChannel;
        }

        UdpIncomingDatagramEvent receive() throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(properties.getDatagramSize());
            SocketAddress sourceAddress = datagramChannel.receive(byteBuffer);
            byteBuffer.flip();
            return new UdpIncomingDatagramEvent(sourceAddress, byteBuffer);
        }
    }

    class Sender {
        private final DatagramChannel datagramChannel;

        Sender(DatagramChannel datagramChannel) {
            this.datagramChannel = datagramChannel;
        }

        void send(UdpOutgoingDatagramEvent udpOutgoingDatagramEvent) throws IOException {
            datagramChannel.send(udpOutgoingDatagramEvent.getDatagram(), udpOutgoingDatagramEvent.getTargetAddress());
        }
    }
}
