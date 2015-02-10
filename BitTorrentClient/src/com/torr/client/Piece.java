package com.torr.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import com.torr.msgs.PeerMessage;
import com.torr.utils.HashingUtils;


public class Piece {
	
	private int index;
	private int offset;
	private int length;
	
	private ByteBuffer dataBuffer;
	private TorrentFile torrentFile;
	
	private byte[] hash;
	private boolean valid;
	public enum States {UNAVAILABLE, AVAILABLE, DOWNLOADED };
	private States state;
	private HashMap<String, Peer> seedingPeers = new HashMap<String, Peer>();
	
	public Piece(TorrentFile torrentFile, int index, int offset, int length, byte[] hash)
	{
		this(index, offset, length, hash);
		setTorrentFile(torrentFile);
	}
	
	public Piece(int index, int offset, int length, byte[] hash)
	{
		this.index = index;
		this.offset = offset;
		this.length = length;
		this.hash = hash;
		this.valid = false;
		this.state = States.UNAVAILABLE;
	}
	
	public void setTorrentFile(TorrentFile torrentFile)
	{
		this.torrentFile = torrentFile;		
	}
	public void setState(States state) {
		this.state = state;
		if(state == States.DOWNLOADED)
		{
			seedingPeers.clear();
		}
			
	}
	public States getState() {
		return state;
	}
	public int getIndex()
	{
		return this.index;
	}
	public int getOffset()
	{
		return this.offset;
	}
	public int getLength()
	{
		return this.length;
	}
	public boolean isValid()
	{
		return this.valid;
	}
	public byte[] getHash()
	{
		return this.hash;
	}
	public final ByteBuffer GetBuffer()
	{
		return this.dataBuffer;
	}
	
	
	// Number of peers in swarm that have the piece
	public int GetSeedingPeersCount()
	{
		return seedingPeers.size();
	}
	
	synchronized
	public void addPeer(Peer peer) 
	{
		this.seedingPeers.put(peer.GetPeerId(), peer);
		state = States.AVAILABLE;
	}
	public void deletePeer(Peer peer) 
	{
		this.seedingPeers.remove(peer.GetPeerId());
		if(this.seedingPeers.isEmpty() && this.state != States.DOWNLOADED)
		{
			state = States.UNAVAILABLE;
		}
	}
	
	public ByteBuffer read() throws Exception
	{
		ByteBuffer dataBuffer = readDirect();
		if(!validateDirect(dataBuffer))
			throw new Exception("Piece not valid.");
		
		return dataBuffer;
	}
	
	private ByteBuffer readDirect() throws Exception
	{
		if(this.torrentFile == null)
			throw new IllegalAccessException();
		
		ByteBuffer ret = ByteBuffer.allocate((int)length);
		int bytes = this.torrentFile.read(ret, this.offset);
		if(bytes < 0) bytes = 0;
		
		ret.rewind();
		ret.limit(bytes);
		return ret;		
	}
	
	public ByteBuffer read(long offset, long length) throws IOException {
		
		if (offset + length > this.length) 
		{
			throw new IllegalArgumentException("Invalid data request for Piece #" + this.index + 
					" @ offset=[" + offset + "] length=[" + length + "]");
		}

		ByteBuffer ret = ByteBuffer.allocate((int)length);
		int bytes = this.torrentFile.read(ret, this.offset + offset);
		
		ret.rewind();
		ret.limit(bytes >= 0 ? bytes : 0);
		
		return ret;
	}
	
	public void write(ByteBuffer block, int offset) throws Exception
	{
		if(this.torrentFile == null)
			throw new IllegalAccessException();
		
		ensureDataBufferInitialized();
		
		int blockPos = block.position();
		this.dataBuffer.position(offset);
		this.dataBuffer.put(block);
		block.position(blockPos);
		
		// Check for completion
		int nextOffset = offset + block.remaining();
		
		if(this.length == nextOffset)
		{
			this.torrentFile.NotifyForDownloadedPiece(this);
		}
		
	}
	
	public boolean validate()
	{
		try
		{
			this.valid = validateDirect(this.readDirect());
			if(this.valid)
			{
				this.state = States.DOWNLOADED;
				this.seedingPeers.clear();
			}
		}
		catch(Exception ex)
		{
			this.valid = false;
		}
		return this.valid;
	}
	
	private boolean validateDirect(ByteBuffer dataBuffer)
	{
		try
		{
			this.valid = false;
			
			// Read data from file into buffer
			byte[] dataBytes = new byte[this.getLength()];			
			dataBuffer.get(dataBytes);
			
			// Check against the local hash
			byte[] newHash = HashingUtils.SHA1(dataBytes);			
			this.valid = Arrays.equals(newHash, this.hash);
		}
		catch(Exception ex)
		{
			this.valid = false;
		}
		
		return this.valid;		
	}
	
	
	private void ensureDataBufferInitialized()
	{
		if(this.dataBuffer == null){
			this.dataBuffer = ByteBuffer.allocate((int)this.getLength());
		}
	}
	
	
	
}
