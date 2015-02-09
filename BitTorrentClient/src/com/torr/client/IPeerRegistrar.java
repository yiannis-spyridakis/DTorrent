package com.torr.client;

import com.torr.msgs.HandshakeMessage;

public interface IPeerRegistrar {
	public TorrentFile RegisterPeer(Peer peer, HandshakeMessage handshakeMsg);
}
