package com.omgameserver.engine.networking;

import com.crionuke.bolts.Bolt;
import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.ClientDisconnectedEvent;
import com.omgameserver.engine.events.DisconnectClientRequestEvent;
import com.omgameserver.engine.events.IncomingDatagramEvent;
import com.omgameserver.engine.events.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
@Service
class InputService extends Bolt implements
        IncomingDatagramEvent.Handler,
        DisconnectClientRequestEvent.Handler,
        TickEvent.Handler {
    static private final Logger logger = LoggerFactory.getLogger(InputService.class);

    private final OmgsProperties properties;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Dispatcher dispatcher;
    private final State state;

    InputService(OmgsProperties properties, ThreadPoolTaskExecutor threadPoolTaskExecutor, Dispatcher dispatcher) {
        super("input", properties.getQueueSize());
        this.properties = properties;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.dispatcher = dispatcher;
        this.state = new State();
    }

    @Override
    public void handleIncomingDatagram(IncomingDatagramEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        SocketAddress sourceAddress = event.getSourceAddress();
        ByteBuffer byteBuffer = event.getByteBuffer();
        InputClient inputClient = state.getClientOrCreate(sourceAddress);
        inputClient.handleDatagram(byteBuffer);
    }

    @Override
    public void handleDisconnectClientRequest(DisconnectClientRequestEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        long clientUid = event.getClientUid();
        InputClient disconnectedClient = state.disconnectClient(clientUid);
        if (disconnectedClient != null) {
            dispatcher.dispatch(
                    new ClientDisconnectedEvent(disconnectedClient.getSocketAddress(), clientUid));
            logger.info("{} disconnected by server", disconnectedClient);
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Client with uid={} to disconnect not found", clientUid);
            }
        }
    }

    @Override
    public void handleTick(TickEvent event) throws InterruptedException {
        if (logger.isTraceEnabled()) {
            logger.trace("Handle {}", event);
        }
        List<InputClient> disconnectedClients = state.findDisconnectedClients();
        for (InputClient diconnectedClient : disconnectedClients) {
            dispatcher.dispatch(new ClientDisconnectedEvent(diconnectedClient.getSocketAddress(),
                    diconnectedClient.getClientUid()));
            logger.info("{} timed out", diconnectedClient);
        }
    }

    @PostConstruct
    void postConstruct() {
        threadPoolTaskExecutor.execute(this);
        dispatcher.subscribe(this, IncomingDatagramEvent.class);
        dispatcher.subscribe(this, DisconnectClientRequestEvent.class);
        dispatcher.subscribe(this, TickEvent.class);
    }

    private class State {

        private final Map<SocketAddress, InputClient> clientBySocket;
        private final Map<Long, InputClient> clientByUid;

        State() {
            clientBySocket = new HashMap<>();
            clientByUid = new HashMap<>();
        }

        InputClient getClientOrCreate(SocketAddress socketAddress) {
            InputClient client = clientBySocket.get(socketAddress);
            if (client == null) {
                client = new InputClient(properties, dispatcher, socketAddress);
                clientBySocket.put(socketAddress, client);
                clientByUid.put(client.getClientUid(), client);
                if (logger.isInfoEnabled()) {
                    logger.info("New input client from {} with uid={}", socketAddress, client.getClientUid());
                }
            }
            return client;
        }

        InputClient disconnectClient(long clientUid) {
            InputClient client = clientByUid.get(clientUid);
            if (client != null) {
                clientByUid.remove(clientUid);
                SocketAddress socketAddress = client.getSocketAddress();
                clientBySocket.remove(socketAddress);
            }

            return client;
        }

        List<InputClient> findDisconnectedClients() {
            List<InputClient> disconnectedClients = new ArrayList<>();
            long currentTimeMillis = System.currentTimeMillis();
            Iterator<Map.Entry<SocketAddress, InputClient>> iterator = clientBySocket.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<SocketAddress, InputClient> entry = iterator.next();
                InputClient inputClient = entry.getValue();
                if (inputClient.isDisconnected(currentTimeMillis)) {
                    clientByUid.remove(inputClient.getClientUid());
                    iterator.remove();
                    disconnectedClients.add(inputClient);
                }
            }
            return disconnectedClients;
        }
    }
}
