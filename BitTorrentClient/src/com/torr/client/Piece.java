package com.torr.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import com.torr.msgs.PeerMessage;
import com.torr.policies.ProtocolPolicy;
import com.torr.utils.HashingUtils;


public class Piece {
	
	private int index = 0;
	private int offset = 0;
	private int length = 0;
	private int next_block_offset = 0; // for download requests
	
	private ByteBuffer dataBuffer;
	private TorrentFile torrentFile;
	
	private byte[] hash;
	private volatile boolean valid;
	private volatile int availablePeers = 0;
	private Peer downloadingPeer = null;
	
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
	}
	
	public void setTorrentFile(TorrentFile torrentFile)
	{
		this.torrentFile = torrentFile;		
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
	public void SetDownloadingPeer(Peer peer)
	{
		this.downloadingPeer = peer;
	}
	public Peer GetDownloadingPeer()
	{
		return this.downloadingPeer;
	}
	
	public PeerMessage.RequestMessage GetNextBlockRequest()
	{
		// Return null if we've requested all blocks
		if(next_block_offset >= this.length)
			return null;
		
		final int length = 
				Math.min(ProtocolPolicy.BLOCK_SIZE, this.length - this.next_block_offset);
		
		PeerMessage.RequestMessage ret = 
				new PeerMessage.RequestMessage(this.index, next_block_offset, length);
		
		next_block_offset += length;
		
		return ret;
	}
	public int GetNextBlockOffset()
	{
		return this.next_block_offset;
	}
	public void SetNextBlockOffset(final int offset)
	{
		this.next_block_offset = offset;
	}
	
	// Number of peers in swarm that have the piece
	public int GetSeedingPeersCount()
	{
		return this.availablePeers;
	}
	public void IncreaseAvailablePeers()
	{
		++this.availablePeers;
	}
	public void DecreaseAvailablePeers()
	{
		--this.availablePeers;
	}
	public boolean IsAvailable()
	{
		return (!this.valid) && (this.availablePeers > 0);
	}
	
	public ByteBuffer read() throws Exception
	{
		ByteBuffer dataBuffer = readDirect();
		if(!validateDirect(dataBuffer))
			throw new Exception("Invalid read request for non-valid piece");
		
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
		
		block.rewind();
		this.dataBuffer.position(offset);
		this.dataBuffer.put(block);
		block.rewind();
		
		// Check for completion
		int nextOffset = offset + block.remaining();
		
		if(nextOffset >= this.length)
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
				this.availablePeers = 0;
				this.dataBuffer = null;
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
