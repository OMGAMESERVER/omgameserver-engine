package com.omgameserver.engine.udp.events;

import com.crionuke.bolts.Event;

import java.net.SocketAddress;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public final class UdpIncomingHeaderEvent extends Event<UdpIncomingHeaderEvent.Handler> {

    private final SocketAddress socketAddress;
    private final long clientUid;
    private final int seq;
    private final int ack;
    private final int bit;
    private final int sys;

    public UdpIncomingHeaderEvent(SocketAddress socketAddress, long clientUid, int seq, int ack, int bit, int sys) {
        super();
        if (socketAddress == null) {
            throw new NullPointerException("socketAddress is null");
        }
        this.socketAddress = socketAddress;
        this.clientUid = clientUid;
        this.seq = seq;
        this.ack = ack;
        this.bit = bit;
        this.sys = sys;
    }

    @Override
    public void handle(Handler handler) throws InterruptedException {
        handler.handleUdpIncomingHeader(this);
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public long getClientUid() {
        return clientUid;
    }

    public int getSeq() {
        return seq;
    }

    public int getAck() {
        return ack;
    }

    public int getBit() {
        return bit;
    }

    public int getSys() {
        return sys;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(socketAddress=" + socketAddress
                + ", seq=" + seq
                + ", ack=" + ack
                + ", bit=" + Integer.toBinaryString(bit)
                + ", sys=" + sys + ")";
    }

    public interface Handler {
        void handleUdpIncomingHeader(UdpIncomingHeaderEvent event) throws InterruptedException;
    }
}
