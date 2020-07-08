package com.omgameserver.engine.transmission;

import com.crionuke.bolts.Dispatcher;
import com.omgameserver.engine.OmgsConstants;
import com.omgameserver.engine.OmgsProperties;
import com.omgameserver.engine.events.OutgoingPayloadEvent;
import com.omgameserver.engine.events.OutgoingRawDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class OutputClient implements OmgsConstants {
    static private final Logger logger = LoggerFactory.getLogger(OutputClient.class);

    private OmgsProperties properties;
    private Dispatcher dispatcher;
    private SocketAddress socketAddress;
    private long clientUid;
    private List<OutgoingPayloadEvent> payloadEvents;
    private Map<Integer, List<OutgoingPayloadEvent>> savedEvents;
    private List<Integer> outgoingSeq;
    private int lastOutgoingSeq;
    private int lastIncomingSeq;
    private int lastIncomingBit;
    private long lastPingRequest;
    private long lastLatency;

    OutputClient(OmgsProperties properties, Dispatcher dispatcher, SocketAddress socketAddress, long clientUid) {
        super();
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.socketAddress = socketAddress;
        this.clientUid = clientUid;
        lastIncomingSeq = 0;
        lastOutgoingSeq = 0;
        lastIncomingBit = 0;
        lastPingRequest = System.currentTimeMillis();
        lastLatency = 0;
        payloadEvents = new LinkedList<>();
        savedEvents = new HashMap<>();
        outgoingSeq = new LinkedList<>();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + socketAddress + ")";
    }

    long getClientUid() {
        return clientUid;
    }

    void handleHeader(int seq, int ack, int bit, int sys) throws InterruptedException {
        if (seq <= lastIncomingSeq) {
            logger.debug("Wrong header's incomingSeq={} from {}, lastIncomingSeq={}",
                    seq, socketAddress, lastIncomingSeq);
            return;
        }
        if (ack > lastOutgoingSeq || ack < 0) {
            logger.debug("Wrong header's incomingAck={} from {}, lastOutgoingSeq={}",
                    ack, socketAddress, lastOutgoingSeq);
            return;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Got datagram from {} with seq={}, ack={}, bit={}, sys={}",
                    socketAddress, seq, ack, Integer.toBinaryString(bit), sys);
        }
        lastIncomingBit = (lastIncomingBit << seq - lastIncomingSeq) | 1;
        lastIncomingSeq = seq;
        detectMissingSeq(ack, bit);
        if ((sys & HEADER_SYS_PONGRES) > 0) {
            // Calc latency every pong response
            lastLatency = System.currentTimeMillis() - lastPingRequest;
        }
        if ((sys & HEADER_SYS_PINGREQ) > 0) {
            // Response pong to ping request
            pong();
        }
    }

    void send(OutgoingPayloadEvent event) {
        payloadEvents.add(event);
    }

    void flush() throws InterruptedException {
        if (payloadEvents.size() == 0) {
            return;
        } else {
            OutgoingRawDataEvent nextEvent = createNextEvent();
            for (OutgoingPayloadEvent payloadEvent : payloadEvents) {
                ByteBuffer payload = payloadEvent.getPayload();
                if (nextEvent.getRawData().remaining() < payload.remaining()) {
                    // Flush
                    nextEvent.getRawData().flip();
                    dispatcher.dispatch(nextEvent);
                    // Next event
                    nextEvent = createNextEvent();
                }
                nextEvent.getRawData().put(payload);
                if (payloadEvent.isReliable()) {
                    saveEvent(lastOutgoingSeq, payloadEvent);
                }
            }
            // Flush
            nextEvent.getRawData().flip();
            dispatcher.dispatch(nextEvent);
            // Clear
            payloadEvents.clear();
        }
    }

    boolean isPingTime(long currentTimeMillis) {
        return currentTimeMillis - lastPingRequest > properties.getPingInterval();
    }

    void ping() throws InterruptedException {
        ByteBuffer datagram = writeHeader(ByteBuffer.allocate(HEADER_SIZE), HEADER_SYS_PINGREQ);
        datagram.flip();
        dispatcher.dispatch(new OutgoingRawDataEvent(socketAddress, datagram));
        lastPingRequest = System.currentTimeMillis();
    }

    private void pong() throws InterruptedException {
        ByteBuffer datagram = writeHeader(ByteBuffer.allocate(HEADER_SIZE), HEADER_SYS_PONGRES);
        datagram.flip();
        dispatcher.dispatch(new OutgoingRawDataEvent(socketAddress, datagram));
    }

    private void saveEvent(int seq, OutgoingPayloadEvent event) {
        List<OutgoingPayloadEvent> reliableEvents = savedEvents.get(seq);
        if (reliableEvents == null) {
            reliableEvents = new ArrayList<>();
            savedEvents.put(seq, reliableEvents);
        }
        reliableEvents.add(event);
    }

    private void detectMissingSeq(int incomingAck, int incomingBit) throws InterruptedException {
        Iterator<Integer> seqIterator = outgoingSeq.iterator();
        while (seqIterator.hasNext()) {
            Integer seq = seqIterator.next();
            int delta = incomingAck - seq;
            if (delta >= 0) {
                if (delta >= 32 || (incomingBit & (1 << delta)) == 0) {
                    resendEvents(seq);
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Seq={} confirmed from {}", seq, socketAddress);
                    }
                    savedEvents.remove(seq);
                }
                seqIterator.remove();
            }
        }
    }

    private void resendEvents(int seq) {
        List<OutgoingPayloadEvent> events = savedEvents.remove(seq);
        if (events != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Resend events from seq={} for {}", seq, socketAddress);
            }
            for (OutgoingPayloadEvent event : events) {
                event.getPayload().position(0);
                send(event);
            }
        }
    }

    private OutgoingRawDataEvent createNextEvent() {
        ByteBuffer rawData = writeHeader(ByteBuffer.allocate(properties.getDatagramSize()), HEADER_SYS_NOVALUE);
        return new OutgoingRawDataEvent(socketAddress, rawData);
    }

    private ByteBuffer writeHeader(ByteBuffer byteBuffer, byte sysFlags) {
        lastOutgoingSeq++;
        byteBuffer.putInt(lastOutgoingSeq);
        byteBuffer.putInt(lastIncomingSeq);
        byteBuffer.putInt(lastIncomingBit);
        byteBuffer.put(sysFlags);
        // Save seq for next checks
        outgoingSeq.add(lastOutgoingSeq);
        return byteBuffer;
    }
}
