package com.omgameserver.engine.transmission;

import com.omgameserver.engine.EngineProperties;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import com.omgameserver.engine.events.OutgoingDatagramEvent;
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
class Channel implements Header {
    static private final Logger logger = LoggerFactory.getLogger(Channel.class);

    private final EngineProperties properties;
    private final DatagramChannel datagramChannel;
    private final Receiver receiver;
    private final Sender sender;

    Channel(EngineProperties properties) throws IOException {
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

        IncomingDatagramEvent receive() throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(properties.getDatagramSize());
            SocketAddress sourceAddress = datagramChannel.receive(byteBuffer);
            byteBuffer.flip();
            return new IncomingDatagramEvent(sourceAddress, byteBuffer);
        }
    }

    class Sender {
        private final DatagramChannel datagramChannel;

        Sender(DatagramChannel datagramChannel) {
            this.datagramChannel = datagramChannel;
        }

        void send(OutgoingDatagramEvent outgoingDatagramEvent) throws IOException {
            datagramChannel.send(outgoingDatagramEvent.getDatagram(), outgoingDatagramEvent.getTargetAddress());
        }
    }
}
