package com.torr.client;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.torr.bencode.TorrentFileDescriptor;
import com.torr.msgs.MessageToClient;

public class TorrentFile implements Runnable {
	
	
	TorrentFiles torrentFiles = null;
	Piece pieces[] = null;
	TorrentFileStorage torrentStorage = null;
	List<MessageToClient> peers;
	TrackerClient trackerClient = new TrackerClient(Consts.TRACKER_PORT_NUMBER, this);
	TorrentFileDescriptor descriptor;
	
	public TorrentFile(
			TorrentFileDescriptor descriptor, 
			File destinationDir, 
			boolean isSeeder
		)
	{
		//this.pieces = descriptor.getPieces();
	}
	
	public void run() {
		//creates Peers threads, 
		/*for (int i = 0; i < peers.size(); i++) {
			if(!peers.get(i).peer_id.equals(message.peer_id)) {
				Thread t = new Peer(peers.get(i).IP, peers.get(i).peer_id, peers.get(i).port);
				t.start();
			}
		}*/
	
	}
	
	public String getTrackerUrl() {
		return descriptor.TrackerUrl();
	}
	
	public void setPeers(List<MessageToClient> peers) {
		this.peers = peers;
	}
	
	public Piece getPiece(int index)
	{
		if (this.pieces == null) {
			throw new IllegalStateException("Torrent not initialized yet.");
		}

		if (index >= this.pieces.length) {
			throw new IllegalArgumentException("Invalid piece index!");
		}
	
		
		return this.pieces[index];
	}
	public int getPieceCount()
	{
		if(pieces == null)
			return 0;
		return pieces.length;
	}
	
	public int read(ByteBuffer buffer, long offset) throws IOException
	{
		return this.torrentStorage.read(buffer, offset);
		
	}
	
	public int write(ByteBuffer buffer, long offset) throws IOException
	{
		return this.torrentStorage.write(buffer, offset);
	}
	
	
}
