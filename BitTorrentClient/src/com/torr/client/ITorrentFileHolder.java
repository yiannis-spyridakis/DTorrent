package com.torr.client;

import com.torr.msgs.PeerMessage;

public interface ITorrentFileHolder 
{
	public TorrentFile GetTorrentFileByInfoHash(final String infoHash);
}
