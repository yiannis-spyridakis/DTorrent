package com.torr.client;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.torr.utils.HashingUtils;


public class Piece {
	
	private int index;
	private int offset;
	private int length;
	
	private ByteBuffer dataBuffer;
	private TorrentFile torrentFile;
	
	private byte[] hash;
	private boolean valid;
	
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
		//this.dataBuffer = ByteBuffer.allocate(length);
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
			this.dataBuffer.rewind();
			this.torrentFile.write(this.dataBuffer, this.offset);
			this.dataBuffer = null;
		}
		
	}
	
	
	public boolean validate()
	{
		try
		{
			this.valid = validateDirect(this.readDirect());
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
