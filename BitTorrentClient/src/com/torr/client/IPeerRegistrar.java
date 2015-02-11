package com.torr.client;

import com.torr.msgs.PeerMessage;

public interface IPeerRegistrar 
{
	public TorrentFile RegisterPeer(Peer peer, PeerMessage.HandshakeMessage handshakeMsg);
}
